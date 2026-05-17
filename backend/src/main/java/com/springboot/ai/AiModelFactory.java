package com.springboot.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AiModelFactory {

    private final ChatModel fastChatModel;

    private final ChatModel smartChatModel;

    private final EmbeddingModel embeddingModel;

    public AiModelFactory(
            @Qualifier("fastChatModel") ChatModel fastChatModel,
            @Qualifier("smartChatModel") ChatModel smartChatModel,
            EmbeddingModel embeddingModel) {
        this.fastChatModel = fastChatModel;
        this.smartChatModel = smartChatModel;
        this.embeddingModel = embeddingModel;
    }

    public ChatModel getChatModel(AiModelType type) {
        switch (type) {
            case FAST:
                return fastChatModel;
            case SMART:
                return smartChatModel;
            default:
                return fastChatModel;
        }
    }

    public ChatModel getFastChatModel() {
        return fastChatModel;
    }

    public ChatModel getSmartChatModel() {
        return smartChatModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
