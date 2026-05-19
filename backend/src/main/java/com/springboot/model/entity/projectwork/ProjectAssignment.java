package com.springboot.model.entity.projectwork;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_assignment")
public class ProjectAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String title;

    private String description;

    private String requirement;

    /** JSON string with scoring dimension config */
    private String rubricJson;

    private BigDecimal fullScore;

    private LocalDateTime startTime;

    private LocalDateTime deadline;

    /** DRAFT / PUBLISHED / CLOSED */
    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
