package io.binghe.ai.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description Ai消息类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息角色：user（用户）、assistant（AI助手）
     */
    @JsonProperty("role")
    private String role;

    /**
     * 消息内容 - 确保始终为字符串
     */
    @JsonProperty("content")
    private String content;

    /**
     * 消息创建时间
     */
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public AIMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 确保内容始终为有效字符串
     */
    public void setContent(String content) {
        this.content = content != null ? content.trim() : "";
    }

    /**
     * 验证消息是否有效
     */
    public boolean isValid() {
        return role != null && !role.trim().isEmpty() &&
                content != null && !content.trim().isEmpty();
    }
}
