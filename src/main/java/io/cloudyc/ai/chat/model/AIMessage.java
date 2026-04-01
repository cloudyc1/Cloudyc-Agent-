package io.cloudyc.ai.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import io.cloudyc.ai.chat.constants.AIConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public AIMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : "";
    }

    public boolean isValid() {
        return role != null && !role.trim().isEmpty() &&
                content != null && !content.trim().isEmpty();
    }

    public static AIMessage fromChatMessage(ChatMessage chatMessage) {
        String role;
        String content;
        if (chatMessage instanceof UserMessage userMessage) {
            role = AIConstants.ROLE_USER;
            content = userMessage.singleText() != null ? userMessage.singleText() : "";
        } else if (chatMessage instanceof AiMessage aiMessage) {
            role = AIConstants.ROLE_ASSISTANT;
            content = aiMessage.text() != null ? aiMessage.text() : "";
        } else {
            return null;
        }
        return new AIMessage(role, content);
    }
}
