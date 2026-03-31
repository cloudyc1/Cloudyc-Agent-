package io.binghe.ai.chat.controller;

import io.binghe.ai.chat.constants.AIConstants;
import io.binghe.ai.chat.model.AIMessage;
import io.binghe.ai.chat.model.ChatSession;
import io.binghe.ai.chat.request.MessageRequest;
import io.binghe.ai.chat.response.MessageResponse;
import io.binghe.ai.chat.service.AIChatService;
import io.binghe.ai.chat.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            log.info("收到 {} 用户的请求, 会话ID: {}", request.getUserId(), sessionId);

            if (sessionId == null || sessionId.trim().isEmpty()) {
                ChatSession session = chatSessionService.createSession(request.getUserId(), request.getMessage());
                sessionId = session.getSessionId();
                log.info("创建新会话, sessionId: {}", sessionId);
            }

            String response = aiChatService.sendMessageWithSession(request.getUserId(), sessionId, request.getMessage());

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
