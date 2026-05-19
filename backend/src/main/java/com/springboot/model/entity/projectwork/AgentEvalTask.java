package com.springboot.model.entity.projectwork;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_eval_task")
public class AgentEvalTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** PROJECT / VUE / PYTHON */
    private String taskType;

    /** 关联的 project_submission.id */
    private Long relatedId;

    private String agentName;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String taskStatus;

    private String prompt;

    /** JSON 字符串，存步骤进度、run_log 等 */
    private String inputJson;

    /** JSON 字符串，存 LLM 原始返回 */
    private String outputJson;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
