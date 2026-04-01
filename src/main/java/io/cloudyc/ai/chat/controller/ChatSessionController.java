package io.cloudyc.ai.chat.controller;

import io.cloudyc.ai.chat.model.ChatSession;
import io.cloudyc.ai.chat.response.MessageResponse;
import io.cloudyc.ai.chat.service.ChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat/session")
@CrossOrigin(origins = "*")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 获取用户的所有对话会话列表
     */
    @GetMapping("/list/{userId}")
    public ResponseEntity<MessageResponse<List<ChatSession>>> getUserSessions(@PathVariable String userId) {
        try {
            log.info("获取用户对话列表, userId: {}", userId);
            List<ChatSession> sessions = chatSessionService.getUserSessions(userId);
            return ResponseEntity.ok(MessageResponse.success(sessions));
        } catch (Exception e) {
            log.error("获取用户对话列表失败, userId: {}, error: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("获取对话列表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取单个会话详情
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<MessageResponse<ChatSession>> getSession(@PathVariable String sessionId) {
        try {
            log.info("获取会话详情, sessionId: {}", sessionId);
            ChatSession session = chatSessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(MessageResponse.success(session));
        } catch (Exception e) {
            log.error("获取会话详情失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("获取会话详情失败：" + e.getMessage()));
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/create")
    public ResponseEntity<MessageResponse<ChatSession>> createSession(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String firstMessage = request.get("firstMessage");

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(MessageResponse.error("用户ID不能为空"));
            }

            log.info("创建新会话, userId: {}, firstMessage: {}", userId, firstMessage);
            ChatSession session = chatSessionService.createSession(userId, firstMessage);

            if (session == null) {
                return ResponseEntity.internalServerError()
                        .body(MessageResponse.error("创建会话失败"));
            }

            return ResponseEntity.ok(MessageResponse.success(session));
        } catch (Exception e) {
            log.error("创建会话失败, error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("创建会话失败：" + e.getMessage()));
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<MessageResponse<String>> deleteSession(
            @PathVariable String sessionId,
            @RequestParam String userId) {
        try {
            log.info("删除会话, sessionId: {}, userId: {}", sessionId, userId);
            chatSessionService.deleteSession(sessionId, userId);
            return ResponseEntity.ok(MessageResponse.success("会话已删除"));
        } catch (Exception e) {
            log.error("删除会话失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("删除会话失败：" + e.getMessage()));
        }
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/{sessionId}/title")
    public ResponseEntity<MessageResponse<String>> updateTitle(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String title = request.get("title");

            if (userId == null || title == null) {
                return ResponseEntity.badRequest()
                        .body(MessageResponse.error("用户ID和标题不能为空"));
            }

            log.info("更新会话标题, sessionId: {}, userId: {}, title: {}", sessionId, userId, title);
            chatSessionService.updateSessionTitle(sessionId, userId, title);
            return ResponseEntity.ok(MessageResponse.success("标题已更新"));
        } catch (Exception e) {
            log.error("更新会话标题失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("更新标题失败：" + e.getMessage()));
        }
    }

    /**
     * 生成标题
     */
    @PostMapping("/generate-title")
    public ResponseEntity<MessageResponse<Map<String, String>>> generateTitle(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(MessageResponse.error("消息内容不能为空"));
            }

            String title = chatSessionService.generateTitle(message);
            Map<String, String> result = new HashMap<>();
            result.put("title", title);
            return ResponseEntity.ok(MessageResponse.success(result));
        } catch (Exception e) {
            log.error("生成标题失败, error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("生成标题失败：" + e.getMessage()));
        }
    }
}
