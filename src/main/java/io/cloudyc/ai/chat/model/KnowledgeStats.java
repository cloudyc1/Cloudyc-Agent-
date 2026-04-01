package io.cloudyc.ai.chat.model;

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
