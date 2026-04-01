package io.binghe.ai.chat.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.binghe.ai.chat.model.DocumentInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class DocumentRepository {

    private static final String DOCUMENT_KEY_PREFIX = "chat:documents:";
    private static final String DOCUMENT_CONTENT_PREFIX = "chat:document:content:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 保存文档信息
     */
    public void saveDocument(String userId, DocumentInfo documentInfo, String content) {
        try {
            String key = DOCUMENT_KEY_PREFIX + userId;
            String docJson = objectMapper.writeValueAsString(documentInfo);
            redisTemplate.opsForHash().put(key, documentInfo.getFileName(), docJson);

            // 同时保存文档内容
            String contentKey = DOCUMENT_CONTENT_PREFIX + userId + ":" + documentInfo.getFileName();
            redisTemplate.opsForValue().set(contentKey, content);

            log.info("文档信息已保存到 Redis: userId={}, fileName={}", userId, documentInfo.getFileName());
        } catch (Exception e) {
            log.error("保存文档信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存文档信息失败", e);
        }
    }

    /**
     * 获取用户的所有文档
     */
    public List<DocumentInfo> getUserDocuments(String userId) {
        try {
            String key = DOCUMENT_KEY_PREFIX + userId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

            if (entries == null || entries.isEmpty()) {
                return List.of();
            }

            return entries.values().stream()
                    .map(obj -> {
                        try {
                            return objectMapper.readValue(obj.toString(), DocumentInfo.class);
                        } catch (Exception e) {
                            log.error("解析文档信息失败: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted((d1, d2) -> d2.getUploadTime().compareTo(d1.getUploadTime()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户文档失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取文档内容
     */
    public String getDocumentContent(String userId, String fileName) {
        String contentKey = DOCUMENT_CONTENT_PREFIX + userId + ":" + fileName;
        return redisTemplate.opsForValue().get(contentKey);
    }

    /**
     * 删除单个文档
     */
    public void deleteDocument(String userId, String fileName) {
        try {
            String key = DOCUMENT_KEY_PREFIX + userId;
            redisTemplate.opsForHash().delete(key, fileName);

            // 删除文档内容
            String contentKey = DOCUMENT_CONTENT_PREFIX + userId + ":" + fileName;
            redisTemplate.delete(contentKey);

            log.info("文档已删除: userId={}, fileName={}", userId, fileName);
        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    /**
     * 清空用户所有文档
     */
    public void clearUserDocuments(String userId) {
        try {
            String key = DOCUMENT_KEY_PREFIX + userId;

            // 获取所有文档名，删除对应的内容
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries != null) {
                for (Object fileName : entries.keySet()) {
                    String contentKey = DOCUMENT_CONTENT_PREFIX + userId + ":" + fileName.toString();
                    redisTemplate.delete(contentKey);
                }
            }

            // 删除文档信息
            redisTemplate.delete(key);

            log.info("用户文档已清空: userId={}", userId);
        } catch (Exception e) {
            log.error("清空用户文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空用户文档失败", e);
        }
    }

    /**
     * 获取文档统计
     */
    public Map<String, Integer> getDocumentStats(String userId) {
        List<DocumentInfo> documents = getUserDocuments(userId);
        int documentCount = documents.size();
        int segmentCount = documents.stream().mapToInt(DocumentInfo::getSegmentCount).sum();

        Map<String, Integer> stats = new HashMap<>();
        stats.put("documentCount", documentCount);
        stats.put("segmentCount", segmentCount);
        return stats;
    }
}
