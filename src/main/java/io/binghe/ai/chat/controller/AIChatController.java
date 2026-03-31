package io.binghe.ai.chat.controller;

import io.binghe.ai.chat.constants.AIConstants;
import io.binghe.ai.chat.model.AIMessage;
import io.binghe.ai.chat.request.MessageRequest;
import io.binghe.ai.chat.response.MessageResponse;
import io.binghe.ai.chat.service.AIChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description AIChatController
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Validated
@CrossOrigin(origins = "*")
public class AIChatController {

    @Autowired
    private AIChatService aiChatService;

    /**
     * 发送聊天消息
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
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
     * 获取历史对话消息
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
                AIConstants.SERVICE, "SpringAI DeepSeek Chat",
                AIConstants.TIMESTAMP, String.valueOf(System.currentTimeMillis())
        ));
    }
}
