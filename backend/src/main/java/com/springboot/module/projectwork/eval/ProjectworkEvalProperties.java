package com.springboot.module.projectwork.eval;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "projectwork.eval")
public class ProjectworkEvalProperties {

    /** 容器启动等待超时（秒） */
    private int containerStartupTimeout = 120;

    /** 整体评测任务超时（秒） */
    private int taskTimeout = 180;

    /** 截图前等待（毫秒） */
    private long screenshotWaitMs = 3000;

    /** 容器内存上限（字节） */
    private long containerMemoryLimit = 536870912L;

    /** 解压目录根路径 */
    private String unzipRoot = "storage/project_unzip";

    /** 截图存储根路径 */
    private String screenshotRoot = "storage/screenshots";
}
