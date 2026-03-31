package io.binghe.ai.chat.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description Ai相关的配置
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2000}")
    private Integer maxTokens;

    /**
     * OpenAI API配置 - 用于DeepSeek
     */
    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    /**
     * ChatModel配置 - 使用DeepSeek API
     */
    @Bean
    public ChatModel chatModel(OpenAiApi openAiApi) {
        // 手动处理类型转换以确保兼容性
        String modelName = model != null && !model.isEmpty() ? model : "deepseek-chat";
        Double temp = temperature != null ? temperature : 0.7;
        Integer tokens = maxTokens != null ? maxTokens : 2000;

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(modelName)
                .withTemperature(temp)
                .withMaxTokens(tokens)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }
}
