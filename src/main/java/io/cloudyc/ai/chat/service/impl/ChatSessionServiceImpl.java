package io.cloudyc.ai.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudyc.ai.chat.assistant.ChatAssistant;
import io.cloudyc.ai.chat.model.ChatSession;
import io.cloudyc.ai.chat.service.ChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final String SESSION_KEY_PREFIX = "chat:session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "chat:user:sessions:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ChatAssistant chatAssistant;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ai.chat.session-expire-days:30}")
    private int sessionExpireDays;

    @Override
    public ChatSession createSession(String userId, String firstMessage) {
        try {
            String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String title = generateTitle(firstMessage);

            ChatSession session = new ChatSession(sessionId, userId, title, firstMessage);
            session.setMessageCount(1);

            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            String sessionJson = objectMapper.writeValueAsString(session);

            stringRedisTemplate.opsForValue().set(sessionKey, sessionJson, sessionExpireDays, TimeUnit.DAYS);

            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            stringRedisTemplate.opsForList().leftPush(userSessionsKey, (String) sessionId);
            stringRedisTemplate.expire(userSessionsKey, sessionExpireDays, TimeUnit.DAYS);

            log.info("创建新对话会话成功, userId: {}, sessionId: {}, title: {}", userId, sessionId, title);
            return session;
        } catch (Exception e) {
            log.error("创建对话会话失败, userId: {}, error: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<ChatSession> getUserSessions(String userId) {
        try {
            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            List<String> sessionIds = stringRedisTemplate.opsForList().range(userSessionsKey, 0, -1);

            if (sessionIds == null || sessionIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<ChatSession> sessions = new ArrayList<>();
            for (String sessionId : sessionIds) {
                ChatSession session = getSession(sessionId);
                if (session != null) {
                    sessions.add(session);
                }
            }

            sessions.sort((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime()));
            return sessions;
        } catch (Exception e) {
            log.error("获取用户对话列表失败, userId: {}, error: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public ChatSession getSession(String sessionId) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            String sessionJson = stringRedisTemplate.opsForValue().get(sessionKey);

            if (sessionJson == null || sessionJson.isEmpty()) {
                return null;
            }

            return objectMapper.readValue(sessionJson, ChatSession.class);
        } catch (Exception e) {
            log.error("获取对话会话失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void updateSession(String sessionId, String userId) {
        try {
            ChatSession session = getSession(sessionId);
            if (session != null) {
                session.setUpdateTime(LocalDateTime.now());

                String sessionKey = SESSION_KEY_PREFIX + sessionId;
                String sessionJson = objectMapper.writeValueAsString(session);
                stringRedisTemplate.opsForValue().set(sessionKey, sessionJson, sessionExpireDays, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("更新对话会话失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }
    }

    @Override
    public void deleteSession(String sessionId, String userId) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            stringRedisTemplate.delete(sessionKey);

            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            stringRedisTemplate.opsForList().remove(userSessionsKey, 0, (String) sessionId);

            log.info("删除对话会话成功, sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("删除对话会话失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }
    }

    @Override
    public String generateTitle(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return "新对话";
            }

            String prompt = "请为以下用户问题生成一个简短的标题（不超过10个字），只返回标题内容，不要有任何解释：\n" + message;
            String title = chatAssistant.chat("title_generator", prompt);

            if (title != null && !title.trim().isEmpty()) {
                title = title.trim().replaceAll("[\"'\\n\\r]", "");
                if (title.length() > 20) {
                    title = title.substring(0, 20) + "...";
                }
                return title;
            }
        } catch (Exception e) {
            log.error("生成标题失败, error: {}", e.getMessage());
        }

        if (message.length() > 15) {
            return message.substring(0, 15) + "...";
        }
        return message;
    }

    @Override
    public void updateSessionTitle(String sessionId, String userId, String title) {
        try {
            ChatSession session = getSession(sessionId);
            if (session != null) {
                session.setTitle(title);
                session.setUpdateTime(LocalDateTime.now());

                String sessionKey = SESSION_KEY_PREFIX + sessionId;
                String sessionJson = objectMapper.writeValueAsString(session);
                stringRedisTemplate.opsForValue().set(sessionKey, sessionJson, sessionExpireDays, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("更新会话标题失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }
    }

    @Override
    public void incrementMessageCount(String sessionId, String userId) {
        try {
            ChatSession session = getSession(sessionId);
            if (session != null) {
                session.setMessageCount(session.getMessageCount() + 1);
                session.setUpdateTime(LocalDateTime.now());

                String sessionKey = SESSION_KEY_PREFIX + sessionId;
                String sessionJson = objectMapper.writeValueAsString(session);
                stringRedisTemplate.opsForValue().set(sessionKey, sessionJson, sessionExpireDays, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("增加消息计数失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }
    }
}
