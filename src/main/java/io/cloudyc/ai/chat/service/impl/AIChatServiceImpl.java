package io.cloudyc.ai.chat.service.impl;

import io.cloudyc.ai.chat.assistant.ChatAssistant;
import io.cloudyc.ai.chat.assistant.RAGChatAssistant;
import io.cloudyc.ai.chat.model.AIMessage;
import io.cloudyc.ai.chat.model.ChatSession;
import io.cloudyc.ai.chat.model.KnowledgeDocument;
import io.cloudyc.ai.chat.service.AIChatContextService;
import io.cloudyc.ai.chat.service.AIChatService;
import io.cloudyc.ai.chat.service.ChatSessionService;
import io.cloudyc.ai.chat.service.RAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AIChatServiceImpl implements AIChatService {

    @Autowired
    private ChatAssistant chatAssistant;

    @Autowired
    private AIChatContextService aiChatContextService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private RAGService ragService;

    @Autowired
    private RAGChatAssistant ragChatAssistant;

    @Override
    public String sendMessage(String userId, String userMessage) {
        try {
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            log.info("开始处理聊天请求，用户ID: {}, 请求ID: {}, 消息: {}", userId, requestId, userMessage);

            String response = chatAssistant.chat(userId, userMessage);

            log.info("请求处理完成，用户ID: {}, 请求ID: {}, 回复消息长度: {}", userId, requestId, response.length());
            return response;

        } catch (Exception e) {
            log.error("请求处理失败，用户ID: {}, 消息: {}, 错误: {}", userId, userMessage, e.getMessage(), e);
            return "请求处理异常，请稍后再试。";
        }
    }

    @Override
    public String sendMessageWithSession(String userId, String sessionId, String userMessage) {
        return sendMessageWithSession(userId, sessionId, userMessage, "NORMAL");
    }

    @Override
    public String sendMessageWithSession(String userId, String sessionId, String userMessage, String mode) {
        try {
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            String actualMode = (mode == null || mode.trim().isEmpty()) ? "NORMAL" : mode.toUpperCase();
            log.info("开始处理聊天请求，用户ID: {}, 会话ID: {}, 请求ID: {}, 模式: {}, 消息: {}",
                    userId, sessionId, requestId, actualMode, userMessage);

            ChatSession session = chatSessionService.getSession(sessionId);
            if (session == null) {
                log.warn("会话不存在，创建新会话, userId: {}", userId);
                session = chatSessionService.createSession(userId, userMessage);
                sessionId = session.getSessionId();
            } else {
                chatSessionService.incrementMessageCount(sessionId, userId);
                chatSessionService.updateSession(sessionId, userId);
            }

            String response;
            if ("RAG".equals(actualMode)) {
                log.info("使用RAG模式处理请求");
                response = processWithRAG(userId, sessionId, userMessage);
            } else {
                log.info("使用Normal模式处理请求");
                response = chatAssistant.chat(sessionId, userMessage);
            }

            log.info("请求处理完成，用户ID: {}, 会话ID: {}, 请求ID: {}, 模式: {}, 回复消息长度: {}",
                    userId, sessionId, requestId, actualMode, response.length());
            return response;

        } catch (Exception e) {
            log.error("请求处理失败，用户ID: {}, 会话ID: {}, 消息: {}, 错误: {}",
                    userId, sessionId, userMessage, e.getMessage(), e);
            return "请求处理异常，请稍后再试。";
        }
    }

    private String processWithRAG(String userId, String sessionId, String userMessage) {
        try {
            log.info("RAG模式处理 - 用户ID: {}, 会话ID: {}, 消息: {}", userId, sessionId, userMessage);

            List<KnowledgeDocument> documents = ragService.retrieve(userMessage, userId);

            String knowledgeContext = buildKnowledgeContext(documents);
            log.info("知识库上下文: {}", knowledgeContext);

            String response = ragChatAssistant.chatWithKnowledge(sessionId, userMessage, knowledgeContext);

            log.info("RAG模式处理完成 - 检索文档数: {}, 回复长度: {}", documents.size(), response.length());

            return response;

        } catch (Exception e) {
            log.error("RAG模式处理失败: {}", e.getMessage(), e);
            return "RAG模式处理异常，请稍后再试。错误信息：" + e.getMessage();
        }
    }

    private String buildKnowledgeContext(List<KnowledgeDocument> documents) {
        if (documents.isEmpty()) {
            return "当前知识库中没有相关信息。";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            KnowledgeDocument doc = documents.get(i);
            context.append(String.format("[文档%d - %s (相似度: %.2f)]\n%s\n\n",
                    i + 1, doc.getFileName(), doc.getSimilarity(), doc.getContent()));
        }
        return context.toString();
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

    @Override
    public List<AIMessage> getSessionMessages(String sessionId) {
        try {
            return aiChatContextService.getContextMessage(sessionId);
        } catch (Exception e) {
            log.error("获取会话消息失败，会话ID: {}, 错误: {}", sessionId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
