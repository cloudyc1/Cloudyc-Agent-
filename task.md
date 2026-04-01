# RAG模式切换功能任务清单

## 项目概述
为Spring AI Chat Redis项目添加RAG（检索增强生成）模式切换功能，允许用户在RAG模式和Normal模式之间切换。

## 功能需求

### 1. 前端功能
- [ ] 在聊天头部区域添加RAG模式切换按钮
- [ ] 按钮动态显示当前模式状态（RAG模式/Normal模式）
- [ ] 不同模式显示不同的颜色标识
  - RAG模式：紫色/蓝色渐变
  - Normal模式：绿色/灰色
- [ ] 点击按钮切换模式，并显示切换动画效果
- [ ] 切换模式时显示提示信息
- [ ] 保存用户选择的模式到localStorage

### 2. 后端功能
- [ ] 在MessageRequest中添加mode字段（RAG/NORMAL）
- [ ] 在AIChatService中根据模式选择不同的处理逻辑
- [ ] RAG模式：集成向量检索功能（需要后续实现RAG核心逻辑）
- [ ] Normal模式：保持现有的直接对话功能
- [ ] 添加模式切换的日志记录

### 3. 数据持久化
- [ ] 将用户模式偏好保存到Redis
- [ ] 在用户登录时加载保存的模式偏好
- [ ] 提供模式设置的API接口

## 技术实现细节

### 前端实现
**文件修改：**
- `src/main/resources/static/index.html`
  - 在chat-header中添加模式切换按钮
  
- `src/main/resources/static/css/index.css`
  - 添加模式切换按钮样式
  - 定义RAG模式和Normal模式的不同颜色主题
  - 添加切换动画效果

- `src/main/resources/static/js/index.js`
  - 添加模式状态管理变量（currentMode）
  - 实现toggleMode()函数
  - 在sendMessage()中添加mode参数
  - 添加localStorage保存/加载模式功能
  - 添加模式切换UI更新函数

**按钮设计：**
```
┌─────────────────────┐
│  🔄 模式: RAG       │  ← RAG模式（紫色）
└─────────────────────┘

┌─────────────────────┐
│  🔄 模式: Normal    │  ← Normal模式（绿色）
└─────────────────────┘
```

### 后端实现
**文件修改：**
- `src/main/java/io/binghe/ai/chat/request/MessageRequest.java`
  - 添加mode字段（String类型，默认为"NORMAL"）
  
- `src/main/java/io/binghe/ai/chat/service/AIChatService.java`
  - 修改sendMessageWithSession方法，添加mode参数
  - 根据mode选择不同的处理逻辑
  
- `src/main/java/io/binghe/ai/chat/service/impl/AIChatServiceImpl.java`
  - 实现模式分支逻辑
  - RAG模式预留接口（待后续实现向量检索）

**API设计：**
```
POST /api/chat/send/session
{
  "userId": "user_001",
  "message": "你好",
  "sessionId": "session_123",
  "mode": "RAG"  // 或 "NORMAL"
}

GET /api/chat/mode/{userId}  // 获取用户模式偏好
PUT /api/chat/mode           // 更新模式偏好
```

### 数据模型
**新增字段：**
- ChatSession表添加mode字段（记录会话使用的模式）
- Redis存储用户默认模式偏好

## 实现步骤

### 阶段1：前端UI实现
1. 在index.html的chat-header区域添加模式切换按钮
2. 在index.css中添加按钮样式和颜色主题
3. 在index.js中实现模式切换逻辑

### 阶段2：后端API扩展
1. 修改MessageRequest添加mode字段
2. 扩展AIChatService接口支持模式参数
3. 实现模式分支处理逻辑

### 阶段3：数据持久化
1. 实现模式偏好的Redis存储
2. 添加模式设置API接口
3. 前端集成模式保存/加载功能

### 阶段4：测试验证
1. 测试模式切换UI交互
2. 测试前后端模式参数传递
3. 测试模式持久化功能
4. 验证两种模式下的对话功能

## 注意事项

1. **向后兼容**：mode参数应该有默认值（NORMAL），确保旧版本前端仍能正常工作
2. **用户体验**：模式切换应该流畅，避免页面刷新
3. **错误处理**：处理无效的模式值，默认使用NORMAL模式
4. **日志记录**：记录模式切换和使用的日志，便于调试
5. **RAG预留**：RAG模式的实现可以先返回提示信息，说明功能待开发

## 预期效果

- 用户可以通过点击按钮在RAG模式和Normal模式之间切换
- 按钮颜色和文字会根据当前模式动态变化
- 模式选择会被保存，下次访问时自动加载
- 后端能够根据模式选择不同的处理逻辑
- 为后续RAG功能开发预留接口和扩展点

## 后续扩展

- RAG模式集成向量数据库（如Milvus、Chroma等）
- 添加文档上传和管理功能
- 实现知识库的构建和更新
- 添加RAG模式的配置选项（如检索数量、相似度阈值等）

---

# RAG模式具体实现方案

## 一、RAG架构概述

RAG（Retrieval-Augmented Generation，检索增强生成）模式通过以下流程工作：

```
用户查询 → 向量化检索 → 获取相关文档 → 构建增强提示 → LLM生成回答
```

## 二、技术选型

### 2.1 向量数据库选择

基于项目已使用Redis，推荐以下方案：

**方案A：Redis Vector Search（推荐）**
- 优点：无需额外部署，与现有Redis集成
- 缺点：向量搜索功能相对基础
- 依赖：Redis Stack 7.2+

**方案B：Milvus**
- 优点：专业向量数据库，功能强大
- 缺点：需要额外部署服务
- 依赖：Milvus Standalone/Distributed

**方案C：Chroma**
- 优点：轻量级，易于集成
- 缺点：Python生态为主，Java支持有限

**推荐使用方案A：Redis Vector Search**

### 2.2 嵌入模型选择

**推荐选项：**
1. **text-embedding-3-small** (OpenAI) - 性价比高
2. **bge-large-zh-v1.5** (BAAI) - 中文优化
3. **m3e-base** (moka-ai) - 中文向量模型

**项目选择：** 使用阿里云DashScope的嵌入API（与DeepSeek同源）

## 三、Maven依赖配置

```xml
<properties>
    <langchain4j.version>0.27.1</langchain4j.version>
</properties>

<dependencies>
    <!-- LangChain4j 向量存储支持 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-redis</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>

    <!-- 嵌入模型 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>

    <!-- 文档处理 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-document-parser-apache-pdf</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>

    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-document-parser-apache-tika</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
</dependencies>
```

## 四、配置文件扩展

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 12

langchain4j:
  open-ai:
    chat-model:
      api-key: sk-6f64a2d36aca478f9b2c9c3af6f9df1c
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: deepseek-v3.1
      temperature: 0.7
      max-tokens: 2000

    # 嵌入模型配置
    embedding-model:
      api-key: sk-6f64a2d36aca478f9b2c9c3af6f9df1c
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: text-embedding-v3
      dimensions: 1536

# RAG配置
rag:
  enabled: true
  # 向量检索配置
  retrieval:
    top-k: 5              # 检索最相关的K个文档
    similarity-threshold: 0.7  # 相似度阈值
    max-chunk-size: 500   # 文档分块大小
    chunk-overlap: 50     # 分块重叠大小

  # 知识库配置
  knowledge-base:
    storage-path: ./knowledge-base
    auto-index: true
    index-on-startup: true

ai:
  chat:
    expire-seconds: 2592000
    max-message-size: 20
    session-expire-days: 30
```

## 五、核心类设计

### 5.1 配置类扩展

```java
// src/main/java/io/binghe/ai/chat/config/RAGConfig.java
package io.binghe.ai.chat.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${langchain4j.open-ai.embedding-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.embedding-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.embedding-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.embedding-model.dimensions:1536}")
    private int dimensions;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return RedisEmbeddingStore.builder()
                .host(redisHost)
                .port(redisPort)
                .dimensions(dimensions)
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
```

### 5.2 RAG服务接口

```java
// src/main/java/io/binghe/ai/chat/service/RAGService.java
package io.binghe.ai.chat.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.RelevanceScore;
import dev.langchain4j.store.embedding.filter.Filter;
import io.binghe.ai.chat.model.KnowledgeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RAGService {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${rag.retrieval.top-k:5}")
    private int topK;

    @Value("${rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.retrieval.max-chunk-size:500}")
    private int maxChunkSize;

    @Value("${rag.retrieval.chunk-overlap:50}")
    private int chunkOverlap;

    /**
     * 添加文档到知识库
     */
    public String ingestDocument(String userId, String fileName, String content) {
        try {
            Document document = Document.from(content);

            DocumentSplitter splitter = DocumentSplitters.recursive(maxChunkSize, chunkOverlap);
            List<TextSegment> segments = splitter.split(document);

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            for (TextSegment segment : segments) {
                segment.metadata().put("userId", userId);
                segment.metadata().put("fileName", fileName);
            }

            ingestor.inkest(segments);

            log.info("文档添加成功，用户: {}, 文件: {}, 分段数: {}", userId, fileName, segments.size());
            return "文档添加成功，共 " + segments.size() + " 个分段";

        } catch (Exception e) {
            log.error("文档添加失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档添加失败: " + e.getMessage());
        }
    }

    /**
     * 从文件添加文档
     */
    public String ingestDocumentFromFile(String userId, MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String content = new String(file.getBytes());

            return ingestDocument(userId, fileName, content);

        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 检索相关文档
     */
    public List<KnowledgeDocument> retrieve(String query, String userId) {
        try {
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    embeddingModel.embed(query).content(),
                    topK,
                    Filter.metadataKey("userId").isEqualTo(userId)
            );

            List<KnowledgeDocument> documents = matches.stream()
                    .filter(match -> match.score() >= similarityThreshold)
                    .map(match -> KnowledgeDocument.builder()
                            .content(match.embedded().text())
                            .fileName(match.embedded().metadata().getString("fileName"))
                            .similarity(match.score())
                            .build())
                    .collect(Collectors.toList());

            log.info("检索完成，查询: {}, 找到相关文档: {}", query, documents.size());
            return documents;

        } catch (Exception e) {
            log.error("检索失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 清除用户知识库
     */
    public void clearUserKnowledge(String userId) {
        try {
            Filter filter = Filter.metadataKey("userId").isEqualTo(userId);
            embeddingStore.removeAll(filter);

            log.info("用户知识库清除成功: {}", userId);
        } catch (Exception e) {
            log.error("清除知识库失败: {}", e.getMessage(), e);
            throw new RuntimeException("清除知识库失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库统计信息
     */
    public KnowledgeStats getKnowledgeStats(String userId) {
        try {
            // 这里需要根据实际存储结构实现统计
            return KnowledgeStats.builder()
                    .userId(userId)
                    .documentCount(0)
                    .segmentCount(0)
                    .build();
        } catch (Exception e) {
            log.error("获取统计信息失败: {}", e.getMessage(), e);
            return KnowledgeStats.builder()
                    .userId(userId)
                    .documentCount(0)
                    .segmentCount(0)
                    .build();
        }
    }
}
```

### 5.3 RAG助手接口

```java
// src/main/java/io/binghe/ai/chat/assistant/RAGChatAssistant.java
package io.binghe.ai.chat.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.binghe.ai.chat.model.KnowledgeDocument;

import java.util.List;

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
    String chatWithKnowledge(@UserMessage String message, @dev.langchain4j.service.V("knowledge") String knowledge);
}
```

### 5.4 RAG配置类

```java
// src/main/java/io/binghe/ai/chat/config/RAGAssistantConfig.java
package io.binghe.ai.chat.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.binghe.ai.chat.assistant.RAGChatAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGAssistantConfig {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public RAGChatAssistant ragChatAssistant() {
        return AiServices.builder(RAGChatAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }
}
```

### 5.5 知识文档模型

```java
// src/main/java/io/binghe/ai/chat/model/KnowledgeDocument.java
package io.binghe.ai.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    private String content;
    private String fileName;
    private double similarity;
}
```

```java
// src/main/java/io/binghe/ai/chat/model/KnowledgeStats.java
package io.binghe.ai.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeStats {
    private String userId;
    private int documentCount;
    private int segmentCount;
}
```

### 5.6 扩展AIChatService

```java
// 在 AIChatService.java 中添加
String sendMessageWithRAG(String userId, String sessionId, String userMessage);
```

```java
// 在 AIChatServiceImpl.java 中实现
@Autowired
private RAGService ragService;

@Autowired
private RAGChatAssistant ragChatAssistant;

@Override
public String sendMessageWithRAG(String userId, String sessionId, String userMessage) {
    try {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        log.info("开始RAG模式处理，用户ID: {}, 会话ID: {}, 请求ID: {}", userId, sessionId, requestId);

        // 1. 检索相关文档
        List<KnowledgeDocument> documents = ragService.retrieve(userMessage, userId);

        // 2. 构建知识库上下文
        String knowledgeContext = buildKnowledgeContext(documents);

        // 3. 使用RAG助手生成回答
        String response = ragChatAssistant.chatWithKnowledge(userMessage, knowledgeContext);

        log.info("RAG模式处理完成，用户ID: {}, 请求ID: {}, 检索文档数: {}, 回复长度: {}",
                userId, requestId, documents.size(), response.length());

        return response;

    } catch (Exception e) {
        log.error("RAG模式处理失败，用户ID: {}, 消息: {}, 错误: {}", userId, userMessage, e.getMessage(), e);
        return "RAG模式处理异常，请稍后再试。";
    }
}

private String buildKnowledgeContext(List<KnowledgeDocument> documents) {
    if (documents.isEmpty()) {
        return "当前知识库中没有相关信息。";
    }

    StringBuilder context = new StringBuilder();
    for (int i = 0; i < documents.size(); i++) {
        KnowledgeDocument doc = documents.get(i);
        context.append(String.format("[文档%d - %s (相似度: %.2f)]\n%s\n\n",
                i + 1, doc.getFileName(), doc.getSimilarity(), doc.getContent()));
    }
    return context.toString();
}
```

### 5.7 扩展Controller

```java
// 在 AIChatController.java 中添加

@Autowired
private RAGService ragService;

/**
 * 上传文档到知识库
 */
@PostMapping("/knowledge/upload")
public ResponseEntity<MessageResponse<String>> uploadDocument(
        @RequestParam String userId,
        @RequestParam MultipartFile file) {
    try {
        String result = ragService.ingestDocumentFromFile(userId, file);
        return ResponseEntity.ok(MessageResponse.success(result));
    } catch (Exception e) {
        log.error("文档上传失败: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(MessageResponse.error("文档上传失败：" + e.getMessage()));
    }
}

/**
 * 添加文本到知识库
 */
@PostMapping("/knowledge/add")
public ResponseEntity<MessageResponse<String>> addKnowledge(
        @RequestBody Map<String, String> request) {
    try {
        String userId = request.get("userId");
        String fileName = request.get("fileName");
        String content = request.get("content");

        String result = ragService.ingestDocument(userId, fileName, content);
        return ResponseEntity.ok(MessageResponse.success(result));
    } catch (Exception e) {
        log.error("添加知识失败: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(MessageResponse.error("添加知识失败：" + e.getMessage()));
    }
}

/**
 * 清除用户知识库
 */
@DeleteMapping("/knowledge/{userId}")
public ResponseEntity<MessageResponse<String>> clearKnowledge(@PathVariable String userId) {
    try {
        ragService.clearUserKnowledge(userId);
        return ResponseEntity.ok(MessageResponse.success("知识库已清除"));
    } catch (Exception e) {
        log.error("清除知识库失败: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(MessageResponse.error("清除知识库失败：" + e.getMessage()));
    }
}

/**
 * 获取知识库统计
 */
@GetMapping("/knowledge/stats/{userId}")
public ResponseEntity<MessageResponse<KnowledgeStats>> getKnowledgeStats(@PathVariable String userId) {
    try {
        KnowledgeStats stats = ragService.getKnowledgeStats(userId);
        return ResponseEntity.ok(MessageResponse.success(stats));
    } catch (Exception e) {
        log.error("获取统计失败: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(MessageResponse.error("获取统计失败：" + e.getMessage()));
    }
}
```

## 六、前端实现

### 6.1 知识库管理界面

```html
<!-- 在 index.html 中添加知识库管理区域 -->
<div class="knowledge-panel" id="knowledgePanel">
    <div class="panel-header">
        <h3>📚 知识库管理</h3>
        <button class="close-panel" onclick="toggleKnowledgePanel()">✕</button>
    </div>
    <div class="panel-content">
        <div class="upload-area">
            <input type="file" id="fileInput" accept=".txt,.md,.pdf,.doc,.docx" style="display: none;">
            <button class="upload-btn" onclick="document.getElementById('fileInput').click()">
                <span>📁</span> 上传文档
            </button>
            <button class="add-text-btn" onclick="showAddTextDialog()">
                <span>✏️</span> 添加文本
            </button>
        </div>
        <div class="knowledge-stats" id="knowledgeStats">
            <div class="stat-item">
                <span class="stat-label">文档数</span>
                <span class="stat-value" id="docCount">0</span>
            </div>
            <div class="stat-item">
                <span class="stat-label">分段数</span>
                <span class="stat-value" id="segmentCount">0</span>
            </div>
        </div>
        <div class="document-list" id="documentList">
            <div class="empty-state">暂无文档</div>
        </div>
        <button class="clear-btn" onclick="clearKnowledge()">
            <span>🗑️</span> 清空知识库
        </button>
    </div>
</div>
```

### 6.2 JavaScript功能

```javascript
// 在 index.js 中添加

let knowledgePanelVisible = false;

function toggleKnowledgePanel() {
    const panel = document.getElementById('knowledgePanel');
    knowledgePanelVisible = !knowledgePanelVisible;
    panel.classList.toggle('visible', knowledgePanelVisible);
    if (knowledgePanelVisible) {
        loadKnowledgeStats();
    }
}

async function uploadDocument(file) {
    const userId = getUserId();
    const formData = new FormData();
    formData.append('userId', userId);
    formData.append('file', file);

    try {
        const resp = await fetch('/api/chat/knowledge/upload', {
            method: 'POST',
            body: formData
        });
        const data = await resp.json();
        if (data.code === 200) {
            alert('文档上传成功：' + data.data);
            loadKnowledgeStats();
        } else {
            alert('上传失败：' + data.message);
        }
    } catch (e) {
        alert('上传失败：' + e.message);
    }
}

async function addTextToKnowledge(fileName, content) {
    const userId = getUserId();
    try {
        const resp = await fetch('/api/chat/knowledge/add', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({userId, fileName, content})
        });
        const data = await resp.json();
        if (data.code === 200) {
            alert('添加成功：' + data.data);
            loadKnowledgeStats();
        } else {
            alert('添加失败：' + data.message);
        }
    } catch (e) {
        alert('添加失败：' + e.message);
    }
}

async function loadKnowledgeStats() {
    const userId = getUserId();
    try {
        const resp = await fetch(`/api/chat/knowledge/stats/${userId}`);
        const data = await resp.json();
        if (data.code === 200) {
            document.getElementById('docCount').textContent = data.data.documentCount;
            document.getElementById('segmentCount').textContent = data.data.segmentCount;
        }
    } catch (e) {
        console.error('加载统计失败:', e);
    }
}

async function clearKnowledge() {
    if (!confirm('确定清空知识库吗？此操作不可恢复！')) return;
    const userId = getUserId();
    try {
        const resp = await fetch(`/api/chat/knowledge/${userId}`, {method: 'DELETE'});
        const data = await resp.json();
        if (data.code === 200) {
            alert('知识库已清空');
            loadKnowledgeStats();
        } else {
            alert('清空失败：' + data.message);
        }
    } catch (e) {
        alert('清空失败：' + e.message);
    }
}

// 文件上传监听
document.getElementById('fileInput').addEventListener('change', function(e) {
    const file = e.target.files[0];
    if (file) {
        uploadDocument(file);
    }
    this.value = '';
});
```

### 6.3 CSS样式

```css
/* 在 index.css 中添加 */

.knowledge-panel {
    position: fixed;
    right: -400px;
    top: 0;
    width: 380px;
    height: 100vh;
    background: #111113;
    border-left: 1px solid rgba(255, 255, 255, 0.1);
    transition: right 0.3s ease;
    z-index: 1000;
    display: flex;
    flex-direction: column;
}

.knowledge-panel.visible {
    right: 0;
}

.panel-header {
    padding: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.panel-header h3 {
    font-size: 16px;
    font-weight: 600;
    color: #e4e4e7;
}

.close-panel {
    background: transparent;
    border: none;
    color: #71717a;
    font-size: 18px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 4px;
}

.close-panel:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #e4e4e7;
}

.panel-content {
    flex: 1;
    padding: 16px;
    overflow-y: auto;
}

.upload-area {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
}

.upload-btn, .add-text-btn {
    flex: 1;
    padding: 12px;
    background: linear-gradient(135deg, #8b5cf6, #7c3aed);
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
}

.upload-btn:hover, .add-text-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.4);
}

.knowledge-stats {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    padding: 12px;
    background: rgba(255, 255, 255, 0.04);
    border-radius: 8px;
}

.stat-item {
    flex: 1;
    text-align: center;
}

.stat-label {
    display: block;
    font-size: 11px;
    color: #71717a;
    margin-bottom: 4px;
}

.stat-value {
    display: block;
    font-size: 20px;
    font-weight: 700;
    color: #8b5cf6;
}

.document-list {
    min-height: 200px;
    margin-bottom: 16px;
}

.empty-state {
    text-align: center;
    color: #52525b;
    font-size: 13px;
    padding: 40px 16px;
}

.clear-btn {
    width: 100%;
    padding: 12px;
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.2);
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
}

.clear-btn:hover {
    background: rgba(239, 68, 68, 0.2);
}
```

## 七、实现步骤

### 阶段1：基础架构搭建
1. 添加Maven依赖
2. 配置Redis Vector Search
3. 创建RAG配置类
4. 实现RAGService基础功能

### 阶段2：文档处理
1. 实现文档上传接口
2. 实现文档分块和嵌入
3. 实现向量存储
4. 实现检索功能

### 阶段3：RAG对话集成
1. 创建RAGChatAssistant
2. 扩展AIChatService
3. 实现模式切换逻辑
4. 添加日志和监控

### 阶段4：前端界面
1. 添加知识库管理面板
2. 实现文档上传功能
3. 实现知识库统计显示
4. 集成模式切换按钮

### 阶段5：测试优化
1. 单元测试
2. 集成测试
3. 性能优化
4. 用户体验优化

## 八、注意事项

1. **Redis版本要求**：需要Redis Stack 7.2+支持向量搜索
2. **嵌入模型选择**：根据实际需求选择合适的嵌入模型
3. **文档分块策略**：合理设置分块大小和重叠度
4. **相似度阈值**：根据实际效果调整相似度阈值
5. **性能优化**：考虑批量嵌入、缓存等优化策略
6. **安全性**：添加文件类型验证、大小限制等安全措施
7. **错误处理**：完善的异常处理和用户提示

## 九、后续扩展

1. **多模态支持**：支持图片、表格等多模态文档
2. **知识库分类**：支持知识库分类和标签
3. **文档版本管理**：支持文档版本控制
4. **权限控制**：实现细粒度的权限管理
5. **智能推荐**：基于用户行为推荐相关文档
6. **对话摘要**：自动生成对话摘要并加入知识库
