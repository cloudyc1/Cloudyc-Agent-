package io.cloudyc.ai.chat.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


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
