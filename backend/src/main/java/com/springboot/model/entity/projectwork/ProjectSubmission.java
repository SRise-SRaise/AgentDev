package com.springboot.model.entity.projectwork;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_submission")
public class ProjectSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long groupId;

    private Long submitStudentId;

    private Long zipFileId;

    /** SUBMITTED / RUNNING / EVALUATED / FAILED / REVIEWED */
    private String submitStatus;

    private LocalDateTime submitTime;

    private String runLog;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
