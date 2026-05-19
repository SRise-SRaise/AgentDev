package com.springboot.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Message body published to eval.task.queue and consumed by EvalTaskConsumer */
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

    /** Absolute path of the ZIP file on local storage */
    private String zipFilePath;

    /** Overall evaluation timeout in seconds */
    private int timeoutSeconds;
}
