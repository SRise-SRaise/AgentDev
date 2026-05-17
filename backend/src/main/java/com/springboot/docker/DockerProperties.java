package com.springboot.docker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "docker")
@Data
public class DockerProperties {

    private String host = "unix:///var/run/docker.sock";

    private boolean tlsVerify = false;

    private String certPath;

    private int maxConnections = 100;

    private long connectionTimeoutSeconds = 30;

    private long responseTimeoutSeconds = 120;
}
