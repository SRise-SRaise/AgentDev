package com.springboot.module.projectwork.eval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.file.FileResourceMapper;
import com.springboot.mapper.projectwork.AgentEvalTaskMapper;
import com.springboot.mapper.projectwork.ProjectAssignmentMapper;
import com.springboot.mapper.projectwork.ProjectSubmissionMapper;
import com.springboot.model.entity.file.FileResource;
import com.springboot.model.entity.projectwork.AgentEvalTask;
import com.springboot.model.entity.projectwork.ProjectAssignment;
import com.springboot.model.entity.projectwork.ProjectSubmission;
import com.springboot.mq.EvalTaskMessage;
import com.springboot.mq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entry point for triggering an Agent evaluation:
 * 1. Validate submission state
 * 2. Insert agent_eval_task (PENDING)
 * 3. Publish message to RabbitMQ
 * 4. Return taskId immediately; frontend polls for status
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalTaskService {

    private final ProjectSubmissionMapper submissionMapper;
    private final ProjectAssignmentMapper assignmentMapper;
    private final AgentEvalTaskMapper agentEvalTaskMapper;
    private final FileResourceMapper fileResourceMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.root-path:storage}")
    private String storageRootPath;

    @Value("${projectwork.eval.task-timeout:180}")
    private int taskTimeoutSeconds;

    @Transactional(rollbackFor = Exception.class)
    public Long triggerEval(Long submissionId) {
        ProjectSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Submission not found");
        }
        if (!"SUBMITTED".equals(submission.getSubmitStatus())
                && !"FAILED".equals(submission.getSubmitStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "Invalid status for evaluation: " + submission.getSubmitStatus());
        }

        ProjectAssignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Assignment not found");
        }

        Long existingTask = checkRunningTask(submissionId);
        if (existingTask != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "Evaluation already in progress: taskId=" + existingTask);
        }

        AgentEvalTask task = new AgentEvalTask();
        task.setTaskType("PROJECT");
        task.setRelatedId(submissionId);
        task.setAgentName("Project Eval Agent");
        task.setTaskStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        try {
            task.setInputJson(objectMapper.writeValueAsString(Map.of(
                    "submissionId", submissionId,
                    "assignmentId", submission.getAssignmentId(),
                    "assignmentTitle", assignment.getTitle()
            )));
        } catch (Exception ignored) {}

        agentEvalTaskMapper.insert(task);
        Long taskId = task.getId();

        submission.setSubmitStatus("RUNNING");
        submission.setUpdatedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);

        String zipFilePath = resolveZipPath(submission.getZipFileId());

        EvalTaskMessage message = EvalTaskMessage.builder()
                .agentTaskId(taskId)
                .submissionId(submissionId)
                .assignmentId(submission.getAssignmentId())
                .zipFilePath(zipFilePath)
                .timeoutSeconds(taskTimeoutSeconds)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVAL_EXCHANGE,
                RabbitMQConfig.EVAL_ROUTING_KEY,
                message);

        log.info("[EvalTaskService] Task queued, taskId={}, submissionId={}", taskId, submissionId);
        return taskId;
    }

    public AgentEvalTask getTaskStatus(Long taskId) {
        AgentEvalTask task = agentEvalTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Task not found");
        }
        return task;
    }

    private Long checkRunningTask(Long submissionId) {
        LambdaQueryWrapper<AgentEvalTask> wrapper = new LambdaQueryWrapper<AgentEvalTask>()
                .eq(AgentEvalTask::getRelatedId, submissionId)
                .eq(AgentEvalTask::getTaskType, "PROJECT")
                .in(AgentEvalTask::getTaskStatus, "PENDING", "RUNNING")
                .orderByDesc(AgentEvalTask::getId)
                .last("LIMIT 1");
        AgentEvalTask existing = agentEvalTaskMapper.selectOne(wrapper);
        return existing != null ? existing.getId() : null;
    }

    private String resolveZipPath(Long zipFileId) {
        if (zipFileId == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Submission has no associated file");
        }
        FileResource fr = fileResourceMapper.selectById(zipFileId);
        if (fr == null || fr.getStoragePath() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,
                    "File record not found for id=" + zipFileId);
        }
        return fr.getStoragePath();
    }
}
