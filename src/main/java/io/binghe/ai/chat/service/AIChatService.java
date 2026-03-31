package io.binghe.ai.chat.service;

import io.binghe.ai.chat.model.AIMessage;

import java.util.List;

public interface AIChatService {

    String sendMessage(String userId, String userMessage);

    String sendMessageWithSession(String userId, String sessionId, String userMessage);

    void clearMessage(String userId);

    List<AIMessage> getMessage(String userId);

    long getMessageSize(String userId);

    List<AIMessage> getSessionMessages(String sessionId);
}
