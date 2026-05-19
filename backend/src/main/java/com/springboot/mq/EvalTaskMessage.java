package com.springboot.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 大作业 Agent 评测消息体
 * 发布到 eval.task.queue，由 EvalTaskConsumer 消费
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalTaskMessage implements Serializable {

    /** agent_eval_task.id */
    private Long agentTaskId;

    /** project_submission.id */
    private Long submissionId;

    /** project_assignment.id */
    private Long assignmentId;

    /** ZIP 文件在本地 storage 的绝对路径 */
    private String zipFilePath;

    /** 整体评测超时秒数（默认 180s） */
    private int timeoutSeconds;
}
