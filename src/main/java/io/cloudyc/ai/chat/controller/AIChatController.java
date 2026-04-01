package io.binghe.ai.chat.controller;

import io.binghe.ai.chat.constants.AIConstants;
import io.binghe.ai.chat.model.AIMessage;
import io.binghe.ai.chat.model.ChatSession;
import io.binghe.ai.chat.model.DocumentInfo;
import io.binghe.ai.chat.model.KnowledgeStats;
import io.binghe.ai.chat.request.MessageRequest;
import io.binghe.ai.chat.response.MessageResponse;
import io.binghe.ai.chat.service.AIChatService;
import io.binghe.ai.chat.service.ChatSessionService;
import io.binghe.ai.chat.service.RAGService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@Validated
@CrossOrigin(origins = "*")
public class AIChatController {

    @Autowired
    private AIChatService aiChatService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private RAGService ragService;

    /**
     * 发送聊天消息（使用用户ID作为上下文）
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse<String>> sendMessage(@Valid @RequestBody MessageRequest request) {
        try {
            log.info("收到 {} 用户的请求", request.getUserId());

            String response = aiChatService.sendMessage(request.getUserId(), request.getMessage());
            return ResponseEntity.ok(MessageResponse.success(response));

        } catch (Exception e) {
            log.error("处理请求失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("处理消息出现错误：" + e.getMessage()));
        }
    }

    /**
     * 发送聊天消息（使用会话ID作为上下文）
     */
    @PostMapping("/send/session")
    public ResponseEntity<MessageResponse<Map<String, Object>>> sendMessageWithSession(@Valid @RequestBody MessageRequest request) {
        try {
            String sessionId = request.getSessionId();
            String mode = request.getMode();
            log.info("收到 {} 用户的请求, 会话ID: {}, 模式: {}, 消息: {}", request.getUserId(), sessionId, mode, request.getMessage());
            log.info("MessageRequest 详情: userId={}, sessionId={}, mode={}, message={}",
                    request.getUserId(), request.getSessionId(), request.getMode(), request.getMessage());

            if (sessionId == null || sessionId.trim().isEmpty()) {
                ChatSession session = chatSessionService.createSession(request.getUserId(), request.getMessage());
                sessionId = session.getSessionId();
                log.info("创建新会话, sessionId: {}", sessionId);
            }

            String response;
            if (mode != null && !mode.trim().isEmpty()) {
                response = aiChatService.sendMessageWithSession(request.getUserId(), sessionId, request.getMessage(), mode);
            } else {
                response = aiChatService.sendMessageWithSession(request.getUserId(), sessionId, request.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("sessionId", sessionId);

            return ResponseEntity.ok(MessageResponse.success(result));

        } catch (Exception e) {
            log.error("处理请求失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("处理消息出现错误：" + e.getMessage()));
        }
    }

    /**
     * 获取历史对话消息（基于用户ID）
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<AIMessage>> getMessage(@PathVariable String userId) {
        try {
            log.info("获取历史对话消息，用户ID: {}", userId);

            List<AIMessage> history = aiChatService.getMessage(userId);
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("获取历史对话消息: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取会话的历史对话消息
     */
    @GetMapping("/history/session/{sessionId}")
    public ResponseEntity<MessageResponse<List<AIMessage>>> getSessionMessages(@PathVariable String sessionId) {
        try {
            log.info("获取会话历史消息，会话ID: {}", sessionId);

            List<AIMessage> history = aiChatService.getSessionMessages(sessionId);
            return ResponseEntity.ok(MessageResponse.success(history));

        } catch (Exception e) {
            log.error("获取会话历史消息失败: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("获取会话历史消息失败：" + e.getMessage()));
        }
    }

    /**
     * 清空历史对话消息
     */
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<Map<String, String>> clearMessage(@PathVariable String userId) {
        try {
            log.info("清空历史对话消息，用户ID: {}", userId);

            aiChatService.clearMessage(userId);
            return ResponseEntity.ok(Map.of(AIConstants.MESSAGE, "对话历史已清除"));

        } catch (Exception e) {
            log.error("清空历史对话消息失败: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "清空历史对话消息失败：" + e.getMessage()));
        }
    }

    /**
     * 获取历史统计信息
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String userId) {
        try {
            log.info("获取历史统计信息，用户ID: {}", userId);

            long historySize = aiChatService.getMessageSize(userId);
            return ResponseEntity.ok(Map.of(
                    AIConstants.USERID, userId,
                    AIConstants.HISTORY_SIZE, historySize,
                    AIConstants.TIMESTAMP, System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("获取历史统计信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 上传文档到知识库
     */
    @PostMapping("/knowledge/upload")
    public ResponseEntity<MessageResponse<String>> uploadDocument(
            @RequestParam String userId,
            @RequestParam MultipartFile file) {
        try {
            String result = ragService.ingestDocumentFromFile(userId, file);
            return ResponseEntity.ok(MessageResponse.success(result));
        } catch (Exception e) {
            log.error("文档上传失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("文档上传失败：" + e.getMessage()));
        }
    }

    /**
     * 添加文本到知识库
     */
    @PostMapping("/knowledge/add")
    public ResponseEntity<MessageResponse<String>> addKnowledge(
            @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String fileName = request.get("fileName");
            String content = request.get("content");

            String result = ragService.ingestDocument(userId, fileName, content);
            return ResponseEntity.ok(MessageResponse.success(result));
        } catch (Exception e) {
            log.error("添加知识失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("添加知识失败：" + e.getMessage()));
        }
    }

    /**
     * 清除用户知识库
     */
    @DeleteMapping("/knowledge/{userId}")
    public ResponseEntity<MessageResponse<String>> clearKnowledge(@PathVariable String userId) {
        try {
            ragService.clearUserKnowledge(userId);
            return ResponseEntity.ok(MessageResponse.success("知识库已清除"));
        } catch (Exception e) {
            log.error("清除知识库失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("清除知识库失败：" + e.getMessage()));
        }
    }

    /**
     * 获取知识库统计
     */
    @GetMapping("/knowledge/stats/{userId}")
    public ResponseEntity<MessageResponse<KnowledgeStats>> getKnowledgeStats(@PathVariable String userId) {
        try {
            KnowledgeStats stats = ragService.getKnowledgeStats(userId);
            return ResponseEntity.ok(MessageResponse.success(stats));
        } catch (Exception e) {
            log.error("获取统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("获取统计失败：" + e.getMessage()));
        }
    }

    /**
     * 获取用户文档列表
     */
    @GetMapping("/knowledge/documents/{userId}")
    public ResponseEntity<MessageResponse<List<DocumentInfo>>> getUserDocuments(@PathVariable String userId) {
        try {
            List<DocumentInfo> documents = ragService.getUserDocuments(userId);
            return ResponseEntity.ok(MessageResponse.success(documents));
        } catch (Exception e) {
            log.error("获取文档列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("获取文档列表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取文档内容
     */
    @GetMapping("/knowledge/document/{userId}")
    public ResponseEntity<MessageResponse<String>> getDocumentContent(
            @PathVariable String userId,
            @RequestParam String fileName) {
        try {
            String content = ragService.getDocumentContent(userId, fileName);
            if (content == null) {
                return ResponseEntity.ok(MessageResponse.error("文档不存在"));
            }
            return ResponseEntity.ok(MessageResponse.success(content));
        } catch (Exception e) {
            log.error("获取文档内容失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("获取文档内容失败：" + e.getMessage()));
        }
    }

    /**
     * 删除单个文档
     */
    @DeleteMapping("/knowledge/document/{userId}")
    public ResponseEntity<MessageResponse<String>> deleteDocument(
            @PathVariable String userId,
            @RequestParam String fileName) {
        try {
            ragService.deleteDocument(userId, fileName);
            return ResponseEntity.ok(MessageResponse.success("文档已删除"));
        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("删除文档失败：" + e.getMessage()));
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                AIConstants.STATUS, AIConstants.STATUS_UP,
                AIConstants.SERVICE, "LangChain4j DeepSeek Chat",
                AIConstants.TIMESTAMP, String.valueOf(System.currentTimeMillis())
        ));
    }
}
