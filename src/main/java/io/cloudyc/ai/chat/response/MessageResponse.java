package io.binghe.ai.chat.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 封装的响应数据
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse<T> {
    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    public static <T> MessageResponse<T> success(T data) {
        return new MessageResponse<>(200, "成功", data, null);
    }

    public static <T> MessageResponse<T> success(T data, String requestId) {
        return new MessageResponse<>(200, "成功", data, requestId);
    }

    public static <T> MessageResponse<T> error(String message) {
        return new MessageResponse<>(500, message, null, null);
    }
}
