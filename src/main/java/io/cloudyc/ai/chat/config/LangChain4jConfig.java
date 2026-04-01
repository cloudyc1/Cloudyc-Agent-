package io.cloudyc.ai.chat.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.cloudyc.ai.chat.assistant.ChatAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private ChatMemoryStore redisChatMemoryStore;

    @Value("${ai.chat.max-message-size:20}")
    private int maxMessages;

    @Bean
    public ChatAssistant chatAssistant() {
        return AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(maxMessages)
                                .chatMemoryStore(redisChatMemoryStore)
                                .build()
                )
                .build();
    }
}
