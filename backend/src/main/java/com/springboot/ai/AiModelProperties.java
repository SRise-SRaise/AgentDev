package com.springboot.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.models")
@Data
public class AiModelProperties {

    private ModelConfig fast = new ModelConfig();

    private ModelConfig smart = new ModelConfig();

    private ModelConfig embedding = new ModelConfig();

    @Data
    public static class ModelConfig {
        private String model;
        private Double temperature;
    }
}
