package com.springboot.model.entity.projectwork;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("agent_eval_report")
public class AgentEvalReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentTaskId;

    private Long projectSubmissionId;

    private String reportTitle;

    private String summary;

    private String advantage;

    private String problem;

    private String suggestion;

    private BigDecimal agentScore;

    /** Structured JSON with dimension scores */
    private String reportJson;

    private Long reportFileId;

    private LocalDateTime createdAt;
}
