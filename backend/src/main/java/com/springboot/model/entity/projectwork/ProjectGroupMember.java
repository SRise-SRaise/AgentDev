package com.springboot.model.entity.projectwork;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_group_member")
public class ProjectGroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long studentId;

    private String roleName;

    private BigDecimal contributionRatio;

    private LocalDateTime createdAt;
}
