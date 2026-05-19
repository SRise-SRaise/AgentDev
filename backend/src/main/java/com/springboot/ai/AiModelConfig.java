package com.springboot.ai;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfig {

    @Bean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean
    @Qualifier("fastChatModel")
    public ChatModel fastChatModel(OpenAiApi openAiApi, AiModelProperties properties) {
        AiModelProperties.ModelConfig config = properties.getFast();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.7)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @Qualifier("smartChatModel")
    public ChatModel smartChatModel(OpenAiApi openAiApi, AiModelProperties properties) {
        AiModelProperties.ModelConfig config = properties.getSmart();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.5)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi, AiModelProperties properties) {
        AiModelProperties.ModelConfig config = properties.getEmbedding();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(config.getModel())
                .build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, options);
    }
}
