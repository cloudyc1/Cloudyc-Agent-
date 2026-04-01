package io.cloudyc.ai.chat.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "langchain4j:chat_memory:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${ai.chat.expire-seconds:3600}")
    private int expireSeconds;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try {
            String key = KEY_PREFIX + memoryId;
            log.info("从Redis获取消息, key: {}", key);
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                log.warn("Redis中没有数据, key: {}", key);
                return Collections.emptyList();
            }
            log.debug("从Redis获取到数据, key: {}, 数据长度: {}", key, json.length());
            List<ChatMessage> messages = messagesFromJson(json);
            log.info("成功解析消息, key: {}, 消息数量: {}", key, messages.size());
            return messages;
        } catch (Exception e) {
            log.error("获取聊天记忆失败, memoryId: {}, 错误: {}", memoryId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            String key = KEY_PREFIX + memoryId;
            String json = messagesToJson(messages);
            stringRedisTemplate.opsForValue().set(key, json, expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("更新聊天记忆失败, memoryId: {}, 错误: {}", memoryId, e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            String key = KEY_PREFIX + memoryId;
            stringRedisTemplate.delete(key);
            log.info("删除聊天记忆成功, memoryId: {}", memoryId);
        } catch (Exception e) {
            log.error("删除聊天记忆失败, memoryId: {}, 错误: {}", memoryId, e.getMessage(), e);
        }
    }
}
