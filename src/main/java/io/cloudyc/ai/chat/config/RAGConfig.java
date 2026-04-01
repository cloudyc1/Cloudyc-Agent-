package io.binghe.ai.chat.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGConfig {

    @Value("${langchain4j.open-ai.embedding-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.embedding-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.embedding-model.model-name}")
    private String modelName;

    @Value("${chroma.host:localhost}")
    private String chromaHost;

    @Value("${chroma.port:8000}")
    private int chromaPort;

    @Value("${chroma.collection:chat-embeddings}")
    private String chromaCollection;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl("http://" + chromaHost + ":" + chromaPort)
                .collectionName(chromaCollection)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
