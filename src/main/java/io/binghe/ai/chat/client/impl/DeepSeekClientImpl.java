package io.binghe.ai.chat.client.impl;

import io.binghe.ai.chat.client.DeepSeekClient;
import io.binghe.ai.chat.constants.AIConstants;
import io.binghe.ai.chat.model.AIMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description DeepSeekClientImpl
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Slf4j
@Component
public class DeepSeekClientImpl implements DeepSeekClient {

    @Autowired
    private ChatModel chatModel;

    @Override
    public String sendMessage(String message) {
        try {
            Prompt prompt = new Prompt(new UserMessage(message));
            ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("调用DeepSeek大模型失败: {}", e.getMessage(), e);
            return "网络异常，请稍后再试。";
        }
    }

    @Override
    public String sendMessageWithContext(List<AIMessage> context) {
        try {
            List<Message> messages = new ArrayList<>();

            for (AIMessage aiMessage : context) {
                if (aiMessage == null || !aiMessage.isValid()) {
                    log.warn("无效消息: {}", aiMessage);
                    continue;
                }

                String role = aiMessage.getRole().trim();
                String content = aiMessage.getContent().trim();

                if (content.isEmpty() || content.length() > 5000) {
                    log.warn("内容为空或内容过长，直接跳过: 角色={}, 长度={}", role, content.length());
                    continue;
                }

                // 判断角色
                if (AIConstants.ROLE_USER.equals(role)) {
                    messages.add(new UserMessage(content));
                } else if (AIConstants.ROLE_ASSISTANT.equals(role)) {
                    messages.add(new AssistantMessage(content));
                } else {
                    log.warn("未知的消息角色: {}, 跳过", role);
                }
            }

            if (messages.isEmpty()) {
                log.warn("消息为空，无需发送");
                return "消息为空，无需发送";
            }

            // 限制消息数量以避免请求过大
            if (messages.size() > 10) {
                messages = messages.subList(messages.size() - 10, messages.size());
                log.info("程序限制上下文消息最多为10条");
            }

            log.info("准备发送 {} 条消息到DeepSeek大模型", messages.size());
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("调用DeepSeek大模型失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
