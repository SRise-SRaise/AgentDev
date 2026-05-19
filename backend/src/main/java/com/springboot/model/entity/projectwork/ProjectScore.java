package com.springboot.model.entity.projectwork;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_score")
public class ProjectScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long groupId;

    private Long studentId;

    private Long submissionId;

    private BigDecimal agentScore;

    private BigDecimal teacherScore;

    private BigDecimal finalScore;

    /** DRAFT / AGENT_SCORED / REVIEWED / CONFIRMED */
    private String scoreStatus;

    private String reviewComment;

    private LocalDateTime confirmedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
