package com.springboot.model.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
@TableName("file_resource")
public class FileResource implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("biz_type")
    private String bizType;

    @TableField("original_name")
    private String originalName;

    @TableField("stored_name")
    private String storedName;

    @TableField("file_ext")
    private String fileExt;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_hash")
    private String fileHash;

    @TableField("storage_path")
    private String storagePath;

    @TableField("upload_user_id")
    private Long uploadUserId;

    @TableField("created_at")
    private Date createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
