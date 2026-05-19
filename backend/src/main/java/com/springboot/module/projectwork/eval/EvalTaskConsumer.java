package com.springboot.module.projectwork.eval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.springboot.module.projectwork.eval.EvalReportParser.ParsedReport;
import com.springboot.module.projectwork.eval.PlaywrightService.ScreenshotResult;
import com.springboot.mq.EvalTaskMessage;
import com.springboot.mq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ 消费者 - 大作业评测状态机
 *
 * 流程：PENDING -> RUNNING
 *   Step 1: 解压 ZIP
 *   Step 2: 启动 Docker 容器（npm install && npm run dev）
 *   Step 3: 等待端口就绪
 *   Step 4: Playwright 截图 + 提取页面文本
 *   Step 5: 日志摘要（fastModel）
 *   Step 6: 主评测（smartModel）
 *   Step 7: 解析 JSON -> 写 agent_eval_report + project_score
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
        log.info("[EvalConsumer] 开始消费任务 taskId={}, submissionId={}", taskId, message.getSubmissionId());

        ContainerContext containerCtx = null;
        try {
            // ---- 更新任务状态 RUNNING ----
            updateTaskStatus(taskId, "RUNNING", null);
            updateSteps(taskId, "初始化", "RUNNING");

            // ---- Step 1: 解压 ZIP ----
            log.info("[EvalConsumer][Step1] 解压 ZIP");
            Path projectDir = dockerRunnerService.unzipProject(message.getZipFilePath());
            String fileTree = promptBuilder.buildFileTree(projectDir);
            updateSteps(taskId, "解压文件", "SUCCESS");

            // ---- Step 2-3: 启动容器并等待就绪 ----
            log.info("[EvalConsumer][Step2] 启动 Docker 容器");
            containerCtx = dockerRunnerService.startContainer(projectDir);
            updateSteps(taskId, "启动容器", "RUNNING");

            log.info("[EvalConsumer][Step3] 等待端口就绪");
            dockerRunnerService.waitForReady(containerCtx);
            updateSteps(taskId, "启动容器", "SUCCESS");

            // 获取运行日志
            String rawLog = dockerRunnerService.getLog(containerCtx.getContainerId());

            // ---- Step 4: Playwright 截图 ----
            log.info("[EvalConsumer][Step4] Playwright 截图");
            updateSteps(taskId, "页面截图", "RUNNING");
            String baseUrl = "http://localhost:" + containerCtx.getHostPort();
            ScreenshotResult screenshot = playwrightService.capture(baseUrl, taskId);
            updateSteps(taskId, "页面截图", "SUCCESS");

            // ---- Step 5: 日志摘要（fastModel）----
            log.info("[EvalConsumer][Step5] 日志摘要（GLM-4-Flash）");
            String logSummaryPrompt = promptBuilder.buildLogSummaryPrompt(rawLog);
            String logSummary = callLlm(fastChatModel, null, logSummaryPrompt);

            // ---- Step 6: 主评测（smartModel）----
            log.info("[EvalConsumer][Step6] 主评测（GLM-4-Plus）");
            updateSteps(taskId, "AI评测中", "RUNNING");
            ProjectAssignment assignment = assignmentMapper.selectById(message.getAssignmentId());
            String evalPrompt = promptBuilder.buildEvalPrompt(
                    assignment, logSummary, screenshot.getPageTextSummary(), fileTree);
            String llmResponse = callLlm(smartChatModel, PromptBuilder.SYSTEM_PROMPT, evalPrompt);

            // ---- Step 7: 解析并持久化 ----
            log.info("[EvalConsumer][Step7] 解析 LLM 返回并写库");
            ParsedReport parsed = reportParser.parse(llmResponse);

            // 写 run_log 到 project_submission
            saveRunLog(message.getSubmissionId(), rawLog,
                    screenshot.getScreenshotPaths(), "EVALUATED");

            // 写 agent_eval_report
            Long reportId = saveEvalReport(taskId, message.getSubmissionId(),
                    assignment.getTitle(), parsed, llmResponse,
                    screenshot.getScreenshotPaths());

            // 写/更新 project_score
            saveProjectScore(message.getSubmissionId(), message.getAssignmentId(),
                    parsed.getAgentScore());

            // 更新任务状态 SUCCESS
            updateTaskStatusSuccess(taskId, llmResponse);
            log.info("[EvalConsumer] 任务完成 taskId={}, score={}", taskId, parsed.getAgentScore());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[EvalConsumer] 任务失败 taskId={}: {}", taskId, e.getMessage(), e);
            updateTaskStatusFailed(taskId, e.getMessage());
            updateSubmissionStatus(message.getSubmissionId(), "FAILED", e.getMessage());
            try {
                // 拒绝消息，不重入主队列（进死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ignored) {}
        } finally {
            // 兜底：无论成败都清理容器
            if (containerCtx != null) {
                dockerRunnerService.cleanup(containerCtx);
            }
        }
    }

    // ---- 私有辅助方法 ----

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

    /** 更新 output_json 中的 steps 进度（供前端轮询展示） */
    private void updateSteps(Long taskId, String stepName, String stepStatus) {
        try {
            AgentEvalTask current = agentEvalTaskMapper.selectById(taskId);
            Map<String, Object> output;
            if (current.getOutputJson() != null) {
                output = objectMapper.readValue(current.getOutputJson(), Map.class);
            } else {
                output = new java.util.LinkedHashMap<>();
            }
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> steps =
                    (java.util.List<Map<String, Object>>) output.computeIfAbsent("steps", k -> new java.util.ArrayList<>());

            // 追加或更新步骤
            boolean found = false;
            for (Map<String, Object> s : steps) {
                if (stepName.equals(s.get("name"))) {
                    s.put("status", stepStatus);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Map<String, Object> newStep = new java.util.LinkedHashMap<>();
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
            log.warn("[EvalConsumer] 更新步骤状态失败: {}", e.getMessage());
        }
    }

    private void saveRunLog(Long submissionId, String runLog,
                              List<String> screenshotPaths, String status) {
        try {
            ProjectSubmission update = new ProjectSubmission();
            update.setId(submissionId);
            update.setSubmitStatus(status);
            update.setRunLog(runLog);
            update.setUpdatedAt(LocalDateTime.now());
            submissionMapper.updateById(update);
        } catch (Exception e) {
            log.error("[EvalConsumer] 保存运行日志失败", e);
        }
    }

    private void updateSubmissionStatus(Long submissionId, String status, String errorMessage) {
        try {
            ProjectSubmission update = new ProjectSubmission();
            update.setId(submissionId);
            update.setSubmitStatus(status);
            update.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                    ? errorMessage.substring(0, 500) : errorMessage);
            update.setUpdatedAt(LocalDateTime.now());
            submissionMapper.updateById(update);
        } catch (Exception e) {
            log.error("[EvalConsumer] 更新 submission 状态失败", e);
        }
    }

    private Long saveEvalReport(Long taskId, Long submissionId, String title,
                                  ParsedReport parsed, String rawJson,
                                  List<String> screenshotPaths) {
        AgentEvalReport report = new AgentEvalReport();
        report.setAgentTaskId(taskId);
        report.setProjectSubmissionId(submissionId);
        report.setReportTitle("【AI评测报告】" + title);
        report.setSummary(parsed.getSummary());
        report.setAdvantage(parsed.getAdvantage());
        report.setProblem(parsed.getProblem());
        report.setSuggestion(parsed.getSuggestion());
        report.setAgentScore(parsed.getAgentScore());
        report.setReportJson(parsed.getRawJson());
        report.setCreatedAt(LocalDateTime.now());
        agentEvalReportMapper.insert(report);
        return report.getId();
    }

    private void saveProjectScore(Long submissionId, Long assignmentId, BigDecimal agentScore) {
        ProjectSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) return;

        // 查是否已有 project_score 记录（按 assignment + student）
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
