package io.cloudyc.ai.chat.service;

import io.cloudyc.ai.chat.model.AIMessage;

import java.util.List;

public interface AIChatContextService {

    List<AIMessage> getContextMessage(String userId);

    void clearContextMessage(String userId);

    long getContextMessageSize(String userId);
}
