package com.springboot.module.projectwork.eval;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 大作业评测专用 AI 配置，独立于全局 spring.ai 配置，
 * 使用 projectwork.ai.* 下的独立 key 和模型，不影响其他模块。
 */
@Configuration
public class ProjectworkAiConfig {

    @Value("${projectwork.ai.base-url}")
    private String baseUrl;

    @Value("${projectwork.ai.api-key}")
    private String apiKey;

    @Value("${projectwork.ai.fast-model:glm-4-flash}")
    private String fastModel;

    @Value("${projectwork.ai.smart-model:glm-4-plus}")
    private String smartModel;

    @Bean("pwFastChatModel")
    public ChatModel pwFastChatModel() {
        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(fastModel)
                .temperature(0.3)
                .build();
        return new OpenAiChatModel(api, options);
    }

    @Bean("pwSmartChatModel")
    public ChatModel pwSmartChatModel() {
        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(smartModel)
                .temperature(0.3)
                .build();
        return new OpenAiChatModel(api, options);
    }
}
