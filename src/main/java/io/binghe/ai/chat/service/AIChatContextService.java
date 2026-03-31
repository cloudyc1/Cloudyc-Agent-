package io.binghe.ai.chat.service;

import io.binghe.ai.chat.constants.AIConstants;
import io.binghe.ai.chat.model.AIMessage;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 上下文Service接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface AIChatContextService {

    /**
     * 将某个用户的消息保存到上下文
     */
    void saveContextMessage(String userId, AIMessage message);

    /**
     * 获取某个用户的上下文消息
     */
    List<AIMessage> getContextMessage(String userId);

    /**
     * 清除某个用户的上下文消息
     */
    void clearContextMessage(String userId);

    /**
     * 获取某个用户的上下文消息大小
     */
    long getContextMessageSize(String userId);

    default String getContextMessageKey(String userId) {
        if (!StringUtils.hasLength(userId)){
            userId = AIConstants.ROLE_USER;
        }
        return AIConstants.AI_CHAT_CONTEXT.concat(userId);
    }
}
