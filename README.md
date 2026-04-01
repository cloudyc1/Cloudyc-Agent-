# Cloudyc-Agent问答助手

基于 Spring Boot + LangChain4j + DeepSeek 的多轮AI智能对话系统，集成 Redis 实现高效的上下文记忆管理，支持 RAG（检索增强生成）知识库问答。

## 功能特性

- 🤖 **智能对话**：基于 DeepSeek 大模型的自然语言对话能力
- 💾 **上下文记忆**：使用 Redis 存储对话历史，支持多轮连贯对话
- 📚 **知识库问答**：RAG 检索增强生成，支持文档上传与智能问答
- 💬 **会话管理**：支持多会话并行，会话历史记录查询
- 📝 **AI生成标题**：自动根据对话内容生成会话标题
- 🌐 **Web界面**：提供友好的前端聊天界面

## 技术栈

| 类别    | 技术                        |
| ----- | ------------------------- |
| 后端框架  | Spring Boot 3.3.5         |
| AI框架  | LangChain4j 1.0.0-beta1   |
| 大模型   | DeepSeek                  |
| 缓存/存储 | Redis                     |
| 文档解析  | Apache PDFBox、Apache Tika |
| 构建工具  | Maven                     |
| JDK版本 | Java 17                   |

## 项目结构

```
spring-ai-chat-redis/
├── src/main/java/io/cloudyc/ai/chat/
│   ├── assistant/          # AI助手接口
│   ├── config/             # 配置类
│   ├── constants/          # 常量定义
│   ├── controller/         # RESTful API控制器
│   ├── memory/             # 对话记忆存储
│   ├── model/              # 数据模型
│   ├── request/            # 请求DTO
│   ├── response/           # 响应DTO
│   └── service/            # 业务逻辑
│       └── impl/           # 服务实现
├── src/main/resources/
│   ├── static/             # 前端静态资源
│   │   ├── css/
│   │   ├── js/
│   │   └── index.html
│   └── application.yml     # 应用配置文件
├── pom.xml
├── LICENSE
└── README.md
```

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- Redis 6.0+

### 2. 克隆项目

```bash
git clone https://github.com/cloudyc/spring-ai-chat-redis.git
cd spring-ai-chat-redis
```

### 3. 配置

修改 `src/main/resources/application.yml`：

```yaml
langchain4j:
  open-ai:
    api-key: your-deepseek-api-key
    base-url: https://api.deepseek.com/v1
    model-name: deepseek-chat

spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### 4. 运行

```bash
mvn spring-boot:run
```

### 5. 访问

打开浏览器访问：<http://localhost:8080>

## API接口

### 聊天接口

| 接口                                      | 方法     | 说明           |
| --------------------------------------- | ------ | ------------ |
| `/api/chat/send`                        | POST   | 发送消息（用户级上下文） |
| `/api/chat/send/session`                | POST   | 发送消息（会话级上下文） |
| `/api/chat/history/{userId}`            | GET    | 获取用户历史消息     |
| `/api/chat/history/session/{sessionId}` | GET    | 获取会话历史消息     |
| `/api/chat/history/{userId}`            | DELETE | 清空用户历史消息     |

### 知识库接口

| 接口                                       | 方法     | 说明       |
| ---------------------------------------- | ------ | -------- |
| `/api/chat/knowledge/upload`             | POST   | 上传文档到知识库 |
| `/api/chat/knowledge/add`                | POST   | 添加文本到知识库 |
| `/api/chat/knowledge/{userId}`           | DELETE | 清除用户知识库  |
| `/api/chat/knowledge/stats/{userId}`     | GET    | 获取知识库统计  |
| `/api/chat/knowledge/documents/{userId}` | GET    | 获取用户文档列表 |

## 核心功能说明

### 1. 多轮对话上下文记忆

系统使用 Redis 存储对话历史，通过 `RedisChatMemoryStore` 实现 LangChain4j 的 `ChatMemoryStore` 接口，支持：

- 消息序列化/反序列化
- TTL自动过期管理
- 用户级与会话级上下文隔离

### 2. RAG知识库问答

实现流程：

1. 文档上传（支持 PDF、TXT 等格式）
2. 文本解析与分块
3. 向量嵌入存储
4. 相似度检索
5. 结合检索结果生成回答

### 3. 会话管理

- 自动创建新会话
- AI生成会话标题
- 消息计数与活跃度追踪
- 会话历史列表查询

## 项目亮点

- **AI工程化实践**：深度集成 LangChain4j 框架，实现与 DeepSeek 大模型的无缝对接
- **高性能上下文管理**：Redis 存储对话历史，支持高并发场景
- **RAG检索增强**：完整的文档处理与向量检索流程
- **企业级设计**：RESTful API 规范、统一响应封装、完善的日志追踪

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 开源协议

Copyright 2024 cloudyc.
