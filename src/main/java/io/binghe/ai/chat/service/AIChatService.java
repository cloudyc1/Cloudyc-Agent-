package io.binghe.ai.chat.service;

import io.binghe.ai.chat.model.AIMessage;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 项目的主要Service接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface AIChatService {

    /**
     * 发送消息的接口
     */
    String sendMessage(String userId, String userMessage);

    /**
     * 清除某个用户的消息
     */
    void clearMessage(String userId);

    /**
     * 获取某个用户的历史消息
     */
    List<AIMessage> getMessage(String userId);

    /**
     * 获取某个用户对话历史的大小
     */
    long getMessageSize(String userId);
}
