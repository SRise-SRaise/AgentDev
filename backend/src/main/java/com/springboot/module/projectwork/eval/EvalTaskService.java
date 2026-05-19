package com.springboot.module.projectwork.eval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.exception.BusinessException;
import com.springboot.common.ErrorCode;
import com.springboot.mapper.projectwork.AgentEvalTaskMapper;
import com.springboot.mapper.projectwork.ProjectAssignmentMapper;
import com.springboot.mapper.projectwork.ProjectSubmissionMapper;
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
 * 评测触发入口：
 * 1. 校验 submission 存在且状态合法
 * 2. 写 agent_eval_task（PENDING）
 * 3. 发消息到 RabbitMQ
 * 4. 立即返回 taskId，前端轮询状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalTaskService {

    private final ProjectSubmissionMapper submissionMapper;
    private final ProjectAssignmentMapper assignmentMapper;
    private final AgentEvalTaskMapper agentEvalTaskMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.root-path:storage}")
    private String storageRootPath;

    @Value("${projectwork.eval.task-timeout:180}")
    private int taskTimeoutSeconds;

    /**
     * 触发 Agent 评测
     * @param submissionId project_submission.id
     * @return agent_eval_task.id
     */
    @Transactional(rollbackFor = Exception.class)
    public Long triggerEval(Long submissionId) {
        // 1. 查 submission
        ProjectSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }
        if (!"SUBMITTED".equals(submission.getSubmitStatus())
                && !"FAILED".equals(submission.getSubmitStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "当前状态不可触发评测: " + submission.getSubmitStatus());
        }

        // 2. 查 assignment（获取评分标准、标题）
        ProjectAssignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "大作业信息不存在");
        }

        // 3. 检查是否已有 PENDING/RUNNING 任务（防止重复触发）
        Long existingTask = checkRunningTask(submissionId);
        if (existingTask != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "已有进行中的评测任务: " + existingTask);
        }

        // 4. 写 agent_eval_task
        AgentEvalTask task = new AgentEvalTask();
        task.setTaskType("PROJECT");
        task.setRelatedId(submissionId);
        task.setAgentName("基于Agent的多文件实验项目评测");
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

        // 5. 更新 submission 状态为 RUNNING
        submission.setSubmitStatus("RUNNING");
        submission.setUpdatedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);

        // 6. 解析 ZIP 文件路径（存储路径由 file_resource 记录，这里简单拼接）
        String zipFilePath = resolveZipPath(submission.getZipFileId());

        // 7. 发 MQ 消息
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

        log.info("[EvalTaskService] 触发评测任务，taskId={}, submissionId={}", taskId, submissionId);
        return taskId;
    }

    /**
     * 查询任务状态（前端轮询用）
     */
    public AgentEvalTask getTaskStatus(Long taskId) {
        AgentEvalTask task = agentEvalTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评测任务不存在");
        }
        return task;
    }

    // ---- 私有工具 ----

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

    /**
     * 从 storage 路径解析 ZIP 文件实际路径。
     * 实际项目中应查 file_resource 表获取 storage_path，这里提供简单实现。
     */
    private String resolveZipPath(Long zipFileId) {
        // TODO: 接入 file_resource 表后替换此处实现
        // 示例：return fileResourceMapper.selectById(zipFileId).getStoragePath();
        return storageRootPath + "/uploads/project_zip/" + zipFileId + ".zip";
    }
}
