package io.binghe.ai.chat.service.impl;

import dev.langchain4j.data.message.ChatMessage;
import io.binghe.ai.chat.memory.RedisChatMemoryStore;
import io.binghe.ai.chat.model.AIMessage;
import io.binghe.ai.chat.service.AIChatContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class AIChatContextServiceImpl implements AIChatContextService {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Override
    public List<AIMessage> getContextMessage(String userId) {
        try {
            log.info("开始获取上下文消息，用户ID: {}", userId);
            List<ChatMessage> chatMessages = redisChatMemoryStore.getMessages(userId);
            log.info("从Redis获取到原始消息，用户ID: {}, 数量: {}", userId, chatMessages == null ? 0 : chatMessages.size());
            
            if (chatMessages == null || chatMessages.isEmpty()) {
                log.warn("Redis中没有找到消息，用户ID: {}", userId);
                return Collections.emptyList();
            }

            List<AIMessage> result = new ArrayList<>();
            for (int i = 0; i < chatMessages.size(); i++) {
                ChatMessage chatMessage = chatMessages.get(i);
                log.debug("处理消息 {}: 类型={}", i, chatMessage.getClass().getSimpleName());
                AIMessage aiMessage = AIMessage.fromChatMessage(chatMessage);
                if (aiMessage != null && aiMessage.isValid()) {
                    result.add(aiMessage);
                    log.debug("消息 {} 转换成功: role={}, content长度={}", i, aiMessage.getRole(), aiMessage.getContent().length());
                } else {
                    log.warn("消息 {} 转换失败或无效", i);
                }
            }

            log.info("成功获取到消息，用户ID: {}, 原始消息数:{}, 有效消息数:{}", userId, chatMessages.size(), result.size());
            return result;
        } catch (Exception e) {
            log.error("获取上下文消息失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void clearContextMessage(String userId) {
        try {
            redisChatMemoryStore.deleteMessages(userId);
            log.info("清空用户上下文消息成功, 用户ID:{}", userId);
        } catch (Exception e) {
            log.error("清空用户上下文失败，用户ID: {}, 错误信息: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    public long getContextMessageSize(String userId) {
        try {
            List<ChatMessage> chatMessages = redisChatMemoryStore.getMessages(userId);
            return chatMessages != null ? chatMessages.size() : 0;
        } catch (Exception e) {
            log.error("获取用户上下文大小失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            return 0;
        }
    }
}
