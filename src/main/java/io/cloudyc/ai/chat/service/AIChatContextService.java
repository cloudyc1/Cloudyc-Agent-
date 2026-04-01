package io.binghe.ai.chat.service;

import io.binghe.ai.chat.model.AIMessage;

import java.util.List;

public interface AIChatContextService {

    List<AIMessage> getContextMessage(String userId);

    void clearContextMessage(String userId);

    long getContextMessageSize(String userId);
}
