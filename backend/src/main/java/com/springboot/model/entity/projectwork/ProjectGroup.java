package com.springboot.model.entity.projectwork;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_group")
public class ProjectGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private String groupName;

    private Long leaderStudentId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
