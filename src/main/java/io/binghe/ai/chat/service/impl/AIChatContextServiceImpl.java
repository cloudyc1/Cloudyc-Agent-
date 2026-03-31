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
            List<ChatMessage> chatMessages = redisChatMemoryStore.getMessages(userId);
            if (chatMessages == null || chatMessages.isEmpty()) {
                return Collections.emptyList();
            }

            List<AIMessage> result = new ArrayList<>();
            for (ChatMessage chatMessage : chatMessages) {
                AIMessage aiMessage = AIMessage.fromChatMessage(chatMessage);
                if (aiMessage != null && aiMessage.isValid()) {
                    result.add(aiMessage);
                }
            }

            log.info("成功获取到消息，用户ID: {}, 消息条数:{}", userId, result.size());
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
