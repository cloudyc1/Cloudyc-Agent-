package io.binghe.ai.chat.service.impl;

import io.binghe.ai.chat.client.DeepSeekClient;
import io.binghe.ai.chat.constants.AIConstants;
import io.binghe.ai.chat.model.AIMessage;
import io.binghe.ai.chat.service.AIChatContextService;
import io.binghe.ai.chat.service.AIChatService;
import io.binghe.ai.chat.utils.IDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Slf4j
@Service
public class AIChatServiceImpl implements AIChatService {

    @Autowired
    private DeepSeekClient deepSeekClient;
    @Autowired
    private AIChatContextService aiChatContextService;

    @Override
    public String sendMessage(String userId, String userMessage) {
        try {
            // 生成请求ID用于追踪
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            log.info("开始处理聊天请求，用户ID: {}, 请求ID: {}, 消息: {}", userId, requestId, userMessage);

            // 创建用户消息对象
            AIMessage aiMessage = new AIMessage(AIConstants.ROLE_USER, userMessage);

            // 将用户消息保存到上下文
            aiChatContextService.saveContextMessage(userId, aiMessage);

            // 获取上下文消息
            List<AIMessage> chatContext = aiChatContextService.getContextMessage(userId);
            log.info("获取到上下文消息，用户ID: {}, 消息数量: {}", userId, chatContext.size());

            if (chatContext.isEmpty()) {
                log.warn("获取到的上下文消息为空");
            }

            // 调用DeepSeek API获取回复
            String response;
            // 如果获取到的上下文消息为空，或者上下文消息中只有一条消息，则直接发送单条消息
            if (chatContext.isEmpty() || chatContext.size() == 1) {
                response = deepSeekClient.sendMessage(userMessage);
            } else {
                // 带上下文发送消息，但如果失败则兜底回退到发送单条消息
                try {
                    response = deepSeekClient.sendMessageWithContext(chatContext);
                } catch (Exception e) {
                    log.warn("上下文消息发送失败，发送单条消息, 用户ID:{}, 错误信息:{}", userId, e.getMessage());
                    response = deepSeekClient.sendMessage(userMessage);
                }
            }

            // 创建AI回复消息对象
            AIMessage assistantMessage = new AIMessage(AIConstants.ROLE_ASSISTANT, response);

            // 将AI回复添加到上下文
            aiChatContextService.saveContextMessage(userId, assistantMessage);

            log.info("请求处理完成，用户ID: {}, 请求ID: {}, 回复消息长度: {}", userId, requestId, response.length());
            return response;

        } catch (Exception e) {
            log.error("请求处理失败，用户ID: {}, 消息: {}, 错误: {}", userId, userMessage, e.getMessage(), e);
            return "请求处理异常，请稍后再试。";
        }
    }

    @Override
    public void clearMessage(String userId) {
        try {
            aiChatContextService.clearContextMessage(userId);
            log.info("清空历史对话成功, 用户ID:{}", userId);
        } catch (Exception e) {
            log.error("清空用户对话历史失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
        }
    }

    @Override
    public List<AIMessage> getMessage(String userId) {
        try {
            List<AIMessage> history = aiChatContextService.getContextMessage(userId);
            log.info("获取历史会话列表，用户ID: {}, 历史会话消息数量: {}", userId, history.size());
            return history;
        } catch (Exception e) {
            log.error("获取历史会话列表失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public long getMessageSize(String userId) {
        try {
            return aiChatContextService.getContextMessageSize(userId);
        } catch (Exception e) {
            log.error("获取历史对话的大小失败，用户ID: {}, 错误: {}", userId, e.getMessage());
            return 0;
        }
    }
}
