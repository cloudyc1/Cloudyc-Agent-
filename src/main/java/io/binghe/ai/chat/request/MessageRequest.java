package io.binghe.ai.chat.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 封装的请求数据
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Data
public class MessageRequest {

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 会话ID（可选）
     */
    private String sessionId;

    /**
     * 用户消息
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 对话模式（NORMAL/RAG）
     */
    private String mode;
}
