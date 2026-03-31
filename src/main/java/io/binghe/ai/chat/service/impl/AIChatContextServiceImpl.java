package io.binghe.ai.chat.service.impl;

import io.binghe.ai.chat.constants.AIConstants;
import io.binghe.ai.chat.model.AIMessage;
import io.binghe.ai.chat.service.AIChatContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 上下文Service实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Slf4j
@Service
public class AIChatContextServiceImpl implements AIChatContextService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Value("${ai.chat.max-message-size:20}")
    private int maxMessageSize;
    @Value("${ai.chat.expire-seconds:3600}")
    private int expireSeconds;

    @Override
    public void saveContextMessage(String userId, AIMessage message) {
        try{
            if (message == null || !message.isValid()) {
                log.warn("消息无效，用户ID: {}, 消息: {}", userId, message);
                return;
            }
            String key = this.getContextMessageKey(userId);
            ListOperations<String, Object> listOperations = redisTemplate.opsForList();
            // 添加消息
            listOperations.rightPush(key, message);
            // 设置过期时间
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);

            // 验证添加是否成功
            Long sizeAfterAdd = listOperations.size(key);

            // 如果消息大小超过配置的阈值，则删除最早的消息
            if (sizeAfterAdd != null && sizeAfterAdd > maxMessageSize) {
                long deleteCount = sizeAfterAdd - maxMessageSize;
                for (int i = 0; i < deleteCount; i++) {
                    listOperations.leftPop(key);
                }
            }
            log.info("消息添加成功, 用户id: {}, 消息大小:{}", userId, listOperations.size(key));
        } catch (Exception e) {
            log.error("消息添加失败，用户id: {}, 错误: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    public List<AIMessage> getContextMessage(String userId) {
        try {
            String key = this.getContextMessageKey(userId);
            ListOperations<String, Object> listOperations = redisTemplate.opsForList();
            Long size = listOperations.size(key);

            if (size != null && size > 0) {
                List<Object> objects = listOperations.range(key, 0, size - 1);

                if (objects != null && !objects.isEmpty()) {
                    List<AIMessage> validMessages = new ArrayList<>();

                    for (int i = 0; i < objects.size(); i++) {
                        Object obj = objects.get(i);

                        try {
                            AIMessage message = null;

                            if (obj instanceof AIMessage) {
                                message = (AIMessage) obj;
                            } else if (obj instanceof Map) {

                                Map<?, ?> map = (Map<?, ?>) obj;

                                String role = map.get(AIConstants.ROLE) != null ? map.get(AIConstants.ROLE).toString() : null;
                                String content = map.get(AIConstants.CONTENT) != null ? map.get(AIConstants.CONTENT).toString() : null;

                                if (role != null && content != null) {
                                    message = new AIMessage(role, content);
                                }
                            } else {
                                log.warn("对象类型不符合预期: {}", obj.getClass());
                            }

                            if (message != null && message.isValid()) {
                                validMessages.add(message);
                            }

                        } catch (Exception objEx) {
                            log.error("处理对象出错, 用户ID：{}, index: {}，错误信息：{}", userId, i + 1, objEx.getMessage());
                        }
                    }
                    log.info("成功获取到消息，用户ID: {}, 消息条数:{}", userId, validMessages.size());
                    return validMessages;
                }
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("获取上下文消息失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void clearContextMessage(String userId) {
        try {
            String key = this.getContextMessageKey(userId);
            redisTemplate.delete(key);
            log.info("清空用户上下文消息成功, 用户ID:{}", userId);
        } catch (Exception e) {
            log.error("清空用户上下文失败，用户ID: {}, 错误信息: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    public long getContextMessageSize(String userId) {
        try {
            String key = this.getContextMessageKey(userId);
            ListOperations<String, Object> listOperations = redisTemplate.opsForList();
            Long size = listOperations.size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取用户上下文大小失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            return 0;
        }
    }
}
