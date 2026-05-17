package com.springboot.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfig {

    @Bean
    @Qualifier("fastChatModel")
    public ChatModel fastChatModel(OpenAiApi openAiApi, AiModelProperties properties) {
        AiModelProperties.ModelConfig config = properties.getFast();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.7)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    @Qualifier("smartChatModel")
    public ChatModel smartChatModel(OpenAiApi openAiApi, AiModelProperties properties) {
        AiModelProperties.ModelConfig config = properties.getSmart();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.5)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }
}
