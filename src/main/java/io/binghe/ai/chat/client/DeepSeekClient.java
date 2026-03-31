package io.binghe.ai.chat.client;

import io.binghe.ai.chat.model.AIMessage;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description DeepSeekClient
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface DeepSeekClient {

    /**
     * 发送消息
     */
    String sendMessage(String message);

    /**
     * 带有上下文的消息发送
     */
    String sendMessageWithContext(List<AIMessage> context);
}
