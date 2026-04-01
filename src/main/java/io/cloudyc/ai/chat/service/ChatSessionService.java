package io.cloudyc.ai.chat.service;

import io.cloudyc.ai.chat.model.ChatSession;

import java.util.List;

public interface ChatSessionService {

    ChatSession createSession(String userId, String firstMessage);

    List<ChatSession> getUserSessions(String userId);

    ChatSession getSession(String sessionId);

    void updateSession(String sessionId, String userId);

    void deleteSession(String sessionId, String userId);

    String generateTitle(String message);

    void updateSessionTitle(String sessionId, String userId, String title);

    void incrementMessageCount(String sessionId, String userId);
}
