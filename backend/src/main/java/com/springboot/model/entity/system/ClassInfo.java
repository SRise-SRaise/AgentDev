package com.springboot.model.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
@TableName("class_info")
public class ClassInfo implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("class_name")
    private String className;

    private String major;

    @TableField("grade_year")
    private String gradeYear;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
