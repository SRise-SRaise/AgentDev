package com.springboot.module.projectwork.eval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.springboot.mapper.projectwork.AgentEvalReportMapper;
import com.springboot.mapper.projectwork.AgentEvalTaskMapper;
import com.springboot.mapper.projectwork.ProjectAssignmentMapper;
import com.springboot.mapper.projectwork.ProjectScoreMapper;
import com.springboot.mapper.projectwork.ProjectSubmissionMapper;
import com.springboot.model.entity.projectwork.AgentEvalReport;
import com.springboot.model.entity.projectwork.AgentEvalTask;
import com.springboot.model.entity.projectwork.ProjectAssignment;
import com.springboot.model.entity.projectwork.ProjectScore;
import com.springboot.model.entity.projectwork.ProjectSubmission;
import com.springboot.module.projectwork.eval.DockerRunnerService.ContainerContext;
import com.springboot.module.projectwork.eval.DockerRunnerService.FullstackContext;
import com.springboot.module.projectwork.eval.EvalReportParser.ParsedReport;
import com.springboot.module.projectwork.eval.PlaywrightService.ScreenshotResult;
import com.springboot.mq.EvalTaskMessage;
import com.springboot.mq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ consumer driving the 7-step evaluation state machine:
 *   PENDING -> RUNNING
 *   Step 1: Unzip
 *   Step 2: Start Docker container
 *   Step 3: Wait for port ready
 *   Step 4: Playwright screenshot + page text
 *   Step 5: Log summary (fastModel)
 *   Step 6: Main evaluation (smartModel)
 *   Step 7: Persist report and score
 *   -> SUCCESS / FAILED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvalTaskConsumer {

    private final DockerRunnerService dockerRunnerService;
    private final PlaywrightService playwrightService;
    private final PromptBuilder promptBuilder;
    private final EvalReportParser reportParser;
    private final ProjectworkEvalProperties props;

    private final AgentEvalTaskMapper agentEvalTaskMapper;
    private final AgentEvalReportMapper agentEvalReportMapper;
    private final ProjectSubmissionMapper submissionMapper;
    private final ProjectAssignmentMapper assignmentMapper;
    private final ProjectScoreMapper projectScoreMapper;
    private final ObjectMapper objectMapper;

    @Qualifier("pwFastChatModel")
    private final ChatModel fastChatModel;

    @Qualifier("pwSmartChatModel")
    private final ChatModel smartChatModel;

    @RabbitListener(
            queues = RabbitMQConfig.EVAL_QUEUE,
            containerFactory = "evalListenerFactory",
            ackMode = "MANUAL"
    )
    public void consume(EvalTaskMessage message,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        Long taskId = message.getAgentTaskId();
        log.info("[EvalConsumer] Start task taskId={}, submissionId={}", taskId, message.getSubmissionId());

        FullstackContext fullstackCtx = null;
        try {
            updateTaskStatus(taskId, "RUNNING", null);
            updateSteps(taskId, "Initializing", "RUNNING");

            // Step 1: Unzip
            log.info("[EvalConsumer][Step1] Unzip");
            Path projectDir = dockerRunnerService.unzipProject(message.getZipFilePath());
            String fileTree = promptBuilder.buildFileTree(projectDir);
            updateSteps(taskId, "Unzip", "SUCCESS");

            // Step 2: Detect DB + start all containers (DB -> backend -> frontend)
            log.info("[EvalConsumer][Step2] Start containers");
            updateSteps(taskId, "Start Containers", "RUNNING");
            fullstackCtx = dockerRunnerService.startContainers(projectDir);

            // Step 2a: Wait for DB sidecar (if any)
            if (fullstackCtx.getDbCtx() != null) {
                log.info("[EvalConsumer][Step2a] Wait for DB sidecar ready");
                updateSteps(taskId, "Start DB", "RUNNING");
                dockerRunnerService.waitForDbReady(
                        fullstackCtx.getDbCtx(), props.getDbStartupTimeout());
                updateSteps(taskId, "Start DB", "SUCCESS");
            } else {
                updateSteps(taskId, "Start DB", "SKIPPED");
            }

            // Step 2b: Wait for backend ready
            if (fullstackCtx.getBackendCtx() != null) {
                log.info("[EvalConsumer][Step2b] Wait for backend ready");
                updateSteps(taskId, "Start Backend", "RUNNING");
                dockerRunnerService.waitForReady(
                        fullstackCtx.getBackendCtx(), props.getBackendStartupTimeout());
                updateSteps(taskId, "Start Backend", "SUCCESS");
            } else {
                updateSteps(taskId, "Start Backend", "SKIPPED");
            }

            // Step 3: Start frontend container + wait ready
            log.info("[EvalConsumer][Step3] Wait for frontend ready");
            updateSteps(taskId, "Start Frontend", "RUNNING");
            dockerRunnerService.waitForReady(fullstackCtx.getFrontendCtx());
            updateSteps(taskId, "Start Frontend", "SUCCESS");

            // Collect logs from all containers
            String frontendLog = dockerRunnerService.getLog(fullstackCtx.getFrontendCtx().getContainerId());
            String backendLog  = fullstackCtx.getBackendCtx() != null
                    ? dockerRunnerService.getLog(fullstackCtx.getBackendCtx().getContainerId())
                    : "";
            String dbLog = fullstackCtx.getDbCtx() != null
                    ? dockerRunnerService.getLog(fullstackCtx.getDbCtx().getContainerId())
                    : "";

            // Step 4: Playwright screenshot (hits frontend; frontend calls backend inside Docker network)
            log.info("[EvalConsumer][Step4] Playwright screenshot");
            updateSteps(taskId, "Screenshot", "RUNNING");
            String frontendUrl = "http://localhost:" + fullstackCtx.getFrontendCtx().getHostPort();
            String backendUrl  = fullstackCtx.getBackendCtx() != null
                    ? "http://localhost:" + fullstackCtx.getBackendCtx().getHostPort()
                    : null;
            ScreenshotResult screenshot = playwrightService.capture(frontendUrl, backendUrl, taskId);
            updateSteps(taskId, "Screenshot", "SUCCESS");

            // Step 5: Log summary (fastModel) — combines frontend + backend + db logs
            log.info("[EvalConsumer][Step5] Log summary");
            String logSummaryPrompt = promptBuilder.buildLogSummaryPrompt(frontendLog, backendLog, dbLog);
            String logSummary = callLlm(fastChatModel, null, logSummaryPrompt);

            // Step 6: Main evaluation (smartModel)
            log.info("[EvalConsumer][Step6] AI Evaluation");
            updateSteps(taskId, "AI Evaluation", "RUNNING");
            ProjectAssignment assignment = assignmentMapper.selectById(message.getAssignmentId());
            String evalPrompt = promptBuilder.buildEvalPrompt(
                    assignment, logSummary, screenshot.getPageTextSummary(), fileTree);
            String llmResponse = callLlm(smartChatModel, PromptBuilder.SYSTEM_PROMPT, evalPrompt);

            // Step 7: Parse and persist
            log.info("[EvalConsumer][Step7] Persist report");
            ParsedReport parsed = reportParser.parse(llmResponse);

            String combinedLog = "[FRONTEND]\n" + frontendLog
                    + "\n[BACKEND]\n" + backendLog
                    + (dbLog.isBlank() ? "" : "\n[DB]\n" + dbLog);
            saveRunLog(message.getSubmissionId(), combinedLog, "EVALUATED");
            saveEvalReport(taskId, message.getSubmissionId(), assignment.getTitle(), parsed);
            saveProjectScore(message.getSubmissionId(), message.getAssignmentId(), parsed.getAgentScore());

            updateTaskStatusSuccess(taskId, llmResponse);
            updateSteps(taskId, "AI Evaluation", "SUCCESS");
            log.info("[EvalConsumer] Task done taskId={}, score={}", taskId, parsed.getAgentScore());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[EvalConsumer] Task failed taskId={}: {}", taskId, e.getMessage(), e);
            updateTaskStatusFailed(taskId, e.getMessage());
            updateSubmissionStatus(message.getSubmissionId(), "FAILED", e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ignored) {}
        } finally {
            if (fullstackCtx != null) {
                dockerRunnerService.cleanup(fullstackCtx);
            }
        }
    }

    // ---- helpers ----

    private String callLlm(ChatModel model, String systemPrompt, String userPrompt) {
        Prompt prompt;
        if (systemPrompt != null) {
            prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        } else {
            prompt = new Prompt(new UserMessage(userPrompt));
        }
        return model.call(prompt).getResult().getOutput().getText();
    }

    private void updateTaskStatus(Long taskId, String status, String errorMessage) {
        AgentEvalTask update = new AgentEvalTask();
        update.setId(taskId);
        update.setTaskStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        if ("RUNNING".equals(status)) {
            update.setStartedAt(LocalDateTime.now());
        }
        if (errorMessage != null) {
            update.setErrorMessage(errorMessage.length() > 1000
                    ? errorMessage.substring(0, 1000) : errorMessage);
        }
        agentEvalTaskMapper.updateById(update);
    }

    private void updateTaskStatusSuccess(Long taskId, String outputJson) {
        AgentEvalTask update = new AgentEvalTask();
        update.setId(taskId);
        update.setTaskStatus("SUCCESS");
        update.setOutputJson(outputJson);
        update.setFinishedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        agentEvalTaskMapper.updateById(update);
    }

    private void updateTaskStatusFailed(Long taskId, String errorMessage) {
        AgentEvalTask update = new AgentEvalTask();
        update.setId(taskId);
        update.setTaskStatus("FAILED");
        update.setFinishedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        if (errorMessage != null) {
            update.setErrorMessage(errorMessage.length() > 1000
                    ? errorMessage.substring(0, 1000) : errorMessage);
        }
        agentEvalTaskMapper.updateById(update);
    }

    @SuppressWarnings("unchecked")
    private void updateSteps(Long taskId, String stepName, String stepStatus) {
        try {
            AgentEvalTask current = agentEvalTaskMapper.selectById(taskId);
            Map<String, Object> output;
            if (current.getOutputJson() != null) {
                output = objectMapper.readValue(current.getOutputJson(), Map.class);
            } else {
                output = new LinkedHashMap<>();
            }
            List<Map<String, Object>> steps =
                    (List<Map<String, Object>>) output.computeIfAbsent("steps", k -> new ArrayList<>());

            boolean found = false;
            for (Map<String, Object> s : steps) {
                if (stepName.equals(s.get("name"))) {
                    s.put("status", stepStatus);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Map<String, Object> newStep = new LinkedHashMap<>();
                newStep.put("name", stepName);
                newStep.put("status", stepStatus);
                steps.add(newStep);
            }

            AgentEvalTask update = new AgentEvalTask();
            update.setId(taskId);
            update.setOutputJson(objectMapper.writeValueAsString(output));
            update.setUpdatedAt(LocalDateTime.now());
            agentEvalTaskMapper.updateById(update);
        } catch (Exception e) {
            log.warn("[EvalConsumer] Failed to update steps: {}", e.getMessage());
        }
    }

    private void saveRunLog(Long submissionId, String runLog, String status) {
        try {
            ProjectSubmission update = new ProjectSubmission();
            update.setId(submissionId);
            update.setSubmitStatus(status);
            update.setRunLog(runLog);
            update.setUpdatedAt(LocalDateTime.now());
            submissionMapper.updateById(update);
        } catch (Exception e) {
            log.error("[EvalConsumer] Failed to save run log", e);
        }
    }

    private void updateSubmissionStatus(Long submissionId, String status, String errorMessage) {
        try {
            ProjectSubmission update = new ProjectSubmission();
            update.setId(submissionId);
            update.setSubmitStatus(status);
            if (errorMessage != null) {
                update.setErrorMessage(errorMessage.length() > 500
                        ? errorMessage.substring(0, 500) : errorMessage);
            }
            update.setUpdatedAt(LocalDateTime.now());
            submissionMapper.updateById(update);
        } catch (Exception e) {
            log.error("[EvalConsumer] Failed to update submission status", e);
        }
    }

    private void saveEvalReport(Long taskId, Long submissionId, String title, ParsedReport parsed) {
        AgentEvalReport report = new AgentEvalReport();
        report.setAgentTaskId(taskId);
        report.setProjectSubmissionId(submissionId);
        report.setReportTitle("[AI Eval] " + title);
        report.setSummary(parsed.getSummary());
        report.setAdvantage(parsed.getAdvantage());
        report.setProblem(parsed.getProblem());
        report.setSuggestion(parsed.getSuggestion());
        report.setAgentScore(parsed.getAgentScore());
        report.setReportJson(parsed.getRawJson());
        report.setCreatedAt(LocalDateTime.now());
        agentEvalReportMapper.insert(report);
    }

    private void saveProjectScore(Long submissionId, Long assignmentId, BigDecimal agentScore) {
        ProjectSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) return;

        LambdaQueryWrapper<ProjectScore> qw = new LambdaQueryWrapper<ProjectScore>()
                .eq(ProjectScore::getAssignmentId, assignmentId)
                .eq(ProjectScore::getStudentId, submission.getSubmitStudentId());
        ProjectScore existing = projectScoreMapper.selectOne(qw);

        if (existing != null) {
            existing.setAgentScore(agentScore);
            existing.setSubmissionId(submissionId);
            existing.setScoreStatus("AGENT_SCORED");
            existing.setUpdatedAt(LocalDateTime.now());
            projectScoreMapper.updateById(existing);
        } else {
            ProjectScore score = new ProjectScore();
            score.setAssignmentId(assignmentId);
            score.setStudentId(submission.getSubmitStudentId());
            score.setGroupId(submission.getGroupId());
            score.setSubmissionId(submissionId);
            score.setAgentScore(agentScore);
            score.setScoreStatus("AGENT_SCORED");
            score.setCreatedAt(LocalDateTime.now());
            score.setUpdatedAt(LocalDateTime.now());
            projectScoreMapper.insert(score);
        }
    }
}
