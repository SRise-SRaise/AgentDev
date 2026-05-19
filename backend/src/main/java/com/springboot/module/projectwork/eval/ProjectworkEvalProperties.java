package com.springboot.module.projectwork.eval;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "projectwork.eval")
public class ProjectworkEvalProperties {

    /** Max seconds to wait for container HTTP port to be ready */
    private int containerStartupTimeout = 120;

    /** Overall task timeout in seconds */
    private int taskTimeout = 180;

    /** Milliseconds to wait before taking screenshots */
    private long screenshotWaitMs = 3000;

    /** Container memory limit in bytes (default 512MB) */
    private long containerMemoryLimit = 536870912L;

    /** Root directory for unzipped project files */
    private String unzipRoot = "storage/project_unzip";

    /** Root directory for screenshot files */
    private String screenshotRoot = "storage/screenshots";
}
