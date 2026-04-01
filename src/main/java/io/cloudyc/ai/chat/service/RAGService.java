package io.binghe.ai.chat.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.binghe.ai.chat.model.DocumentInfo;
import io.binghe.ai.chat.model.KnowledgeDocument;
import io.binghe.ai.chat.model.KnowledgeStats;
import io.binghe.ai.chat.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RAGService {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${rag.retrieval.top-k:5}")
    private int topK;

    @Value("${rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.retrieval.max-chunk-size:500}")
    private int maxChunkSize;

    @Value("${rag.retrieval.chunk-overlap:50}")
    private int chunkOverlap;

    public String ingestDocument(String userId, String fileName, String content) {
        log.info("开始添加文本文档，用户ID: {}, 文件名: {}", userId, fileName);
        try {
            Document document = Document.from(content);

            DocumentSplitter splitter = DocumentSplitters.recursive(maxChunkSize, chunkOverlap);
            List<TextSegment> segments = splitter.split(document);
            log.info("文本分割完成，用户ID: {}, 分段数: {}", userId, segments.size());

            for (TextSegment segment : segments) {
                segment.metadata().put("userId", userId);
                segment.metadata().put("fileName", fileName);
            }

            log.info("开始存储到向量数据库，用户ID: {}, 分段数: {}", userId, segments.size());

            // 为每个分段生成嵌入并存储
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }

            log.info("向量数据库存储完成，用户ID: {}", userId);

            // 保存文档信息到 Redis
            log.info("开始保存文档信息到 Redis，用户ID: {}", userId);
            DocumentInfo documentInfo = DocumentInfo.builder()
                    .fileName(fileName)
                    .segmentCount(segments.size())
                    .uploadTime(LocalDateTime.now())
                    .build();
            documentRepository.saveDocument(userId, documentInfo, content);
            log.info("文档信息保存完成，用户ID: {}", userId);

            log.info("文档添加成功，用户: {}, 文件: {}, 分段数: {}", userId, fileName, segments.size());
            return "文档添加成功，共 " + segments.size() + " 个分段";

        } catch (Exception e) {
            log.error("文档添加失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档添加失败: " + e.getMessage());
        }
    }

    public String ingestDocumentFromFile(String userId, MultipartFile file) {
        log.info("开始处理文档上传，用户ID: {}, 文件名: {}", userId, file.getOriginalFilename());
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("文件名不能为空");
            }

            // 根据文件类型选择合适的解析器
            String lowerFileName = fileName.toLowerCase();
            DocumentParser parser;
            if (lowerFileName.endsWith(".pdf")) {
                parser = new ApachePdfBoxDocumentParser();
            } else if (lowerFileName.endsWith(".docx") || lowerFileName.endsWith(".doc")
                    || lowerFileName.endsWith(".pptx") || lowerFileName.endsWith(".xlsx")
                    || lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md")) {
                parser = new ApacheTikaDocumentParser();
            } else {
                // 默认使用 Tika 解析器
                parser = new ApacheTikaDocumentParser();
            }

            // 使用解析器解析文档
            Document document;
            String content;
            try (InputStream inputStream = file.getInputStream()) {
                document = parser.parse(inputStream);
                content = document.text();
            }
            log.info("文档解析完成，用户ID: {}, 内容长度: {}", userId, content.length());

            // 添加元数据
            document.metadata().put("userId", userId);
            document.metadata().put("fileName", fileName);

            // 分割文档
            DocumentSplitter splitter = DocumentSplitters.recursive(maxChunkSize, chunkOverlap);
            List<TextSegment> segments = splitter.split(document);
            log.info("文档分割完成，用户ID: {}, 分段数: {}", userId, segments.size());

            // 为每个分段添加元数据
            for (TextSegment segment : segments) {
                segment.metadata().put("userId", userId);
                segment.metadata().put("fileName", fileName);
            }

            // 存储到向量数据库
            log.info("开始存储到向量数据库，用户ID: {}, 分段数: {}", userId, segments.size());
            // 为每个分段生成嵌入并存储
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
            log.info("向量数据库存储完成，用户ID: {}", userId);

            // 保存文档信息到 Redis
            log.info("开始保存文档信息到 Redis，用户ID: {}", userId);
            DocumentInfo documentInfo = DocumentInfo.builder()
                    .fileName(fileName)
                    .segmentCount(segments.size())
                    .uploadTime(LocalDateTime.now())
                    .build();
            documentRepository.saveDocument(userId, documentInfo, content);
            log.info("文档信息保存完成，用户ID: {}", userId);

            log.info("文档上传成功，用户: {}, 文件: {}, 分段数: {}", userId, fileName, segments.size());
            return "文档上传成功，共 " + segments.size() + " 个分段";

        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文档处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档处理失败: " + e.getMessage());
        }
    }

    public List<KnowledgeDocument> retrieve(String query, String userId) {
        try {
            log.info("开始检索，查询: {}, 用户ID: {}", query, userId);

            // 先检索所有相关文档（不使用阈值过滤）
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    embeddingModel.embed(query).content(),
                    topK * 2
            );

            log.info("检索到 {} 个匹配（过滤前）", matches.size());

            // 打印所有匹配用于调试
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                String matchUserId = match.embedded().metadata().getString("userId");
                String fileName = match.embedded().metadata().getString("fileName");
                log.info("匹配[{}]: userId={}, fileName={}, score={}", i, matchUserId, fileName, match.score());
            }

            // 先不过滤，看看所有结果
            List<KnowledgeDocument> allDocuments = matches.stream()
                    .map(match -> KnowledgeDocument.builder()
                            .content(match.embedded().text())
                            .fileName(match.embedded().metadata().getString("fileName"))
                            .similarity(match.score())
                            .build())
                    .collect(Collectors.toList());

            log.info("所有匹配文档数: {}", allDocuments.size());

            // 再进行过滤
            List<KnowledgeDocument> documents = matches.stream()
                    .filter(match -> {
                        String matchUserId = match.embedded().metadata().getString("userId");
                        boolean userMatch = matchUserId != null && matchUserId.equals(userId);
                        log.info("用户过滤: matchUserId={}, userId={}, 匹配={}", matchUserId, userId, userMatch);
                        return userMatch;
                    })
                    .filter(match -> {
                        boolean scoreMatch = match.score() >= similarityThreshold;
                        log.info("相似度过滤: score={}, threshold={}, 匹配={}", match.score(), similarityThreshold, scoreMatch);
                        return scoreMatch;
                    })
                    .limit(topK)
                    .map(match -> KnowledgeDocument.builder()
                            .content(match.embedded().text())
                            .fileName(match.embedded().metadata().getString("fileName"))
                            .similarity(match.score())
                            .build())
                    .collect(Collectors.toList());

            log.info("检索完成，查询: {}, 用户ID: {}, 找到相关文档: {}", query, userId, documents.size());
            return documents;

        } catch (Exception e) {
            log.error("检索失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public void clearUserKnowledge(String userId) {
        try {
            // 清除 Redis 中的文档信息
            documentRepository.clearUserDocuments(userId);

            log.info("用户知识库清除成功: {}", userId);
        } catch (Exception e) {
            log.error("清除知识库失败: {}", e.getMessage(), e);
            throw new RuntimeException("清除知识库失败: " + e.getMessage());
        }
    }

    /**
     * 删除单个文档
     */
    public void deleteDocument(String userId, String fileName) {
        try {
            documentRepository.deleteDocument(userId, fileName);
            log.info("文档删除成功: userId={}, fileName={}", userId, fileName);
        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档内容
     */
    public String getDocumentContent(String userId, String fileName) {
        return documentRepository.getDocumentContent(userId, fileName);
    }

    public List<DocumentInfo> getUserDocuments(String userId) {
        log.info("获取用户文档列表，用户ID: {}", userId);
        List<DocumentInfo> documents = documentRepository.getUserDocuments(userId);
        log.info("获取到 {} 个文档，用户ID: {}", documents.size(), userId);
        return documents;
    }

    public KnowledgeStats getKnowledgeStats(String userId) {
        try {
            Map<String, Integer> stats = documentRepository.getDocumentStats(userId);
            return KnowledgeStats.builder()
                    .userId(userId)
                    .documentCount(stats.getOrDefault("documentCount", 0))
                    .segmentCount(stats.getOrDefault("segmentCount", 0))
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
