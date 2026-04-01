package io.cloudyc.ai.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String userId;
    private String title;
    private String firstMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private int messageCount;
    private List<AIMessage> messages;

    public ChatSession(String sessionId, String userId, String title, String firstMessage) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.title = title;
        this.firstMessage = firstMessage;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.messageCount = 0;
    }
}
