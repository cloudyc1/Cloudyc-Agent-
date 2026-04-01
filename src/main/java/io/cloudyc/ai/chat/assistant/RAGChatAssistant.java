package io.cloudyc.ai.chat.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface RAGChatAssistant {

    @SystemMessage("""
        你是一个专业的AI助手，能够基于提供的知识库信息回答用户问题。

        回答规则：
        1. 优先使用提供的知识库信息回答
        2. 如果知识库中没有相关信息，明确告知用户
        3. 引用知识库内容时，标注来源文档
        4. 保持回答的准确性和专业性
        5. 用中文回答用户问题

        知识库信息：
        {{knowledge}}
        """)
    String chatWithKnowledge(@MemoryId String sessionId, @UserMessage String message, @dev.langchain4j.service.V("knowledge") String knowledge);
}
