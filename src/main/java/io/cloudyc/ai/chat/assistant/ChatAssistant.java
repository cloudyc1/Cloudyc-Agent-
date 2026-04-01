package io.cloudyc.ai.chat.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {

    @SystemMessage("你是一个友好且专业的AI助手，请用中文回答用户的问题。")
    String chat(@MemoryId String userId, @UserMessage String message);
}
