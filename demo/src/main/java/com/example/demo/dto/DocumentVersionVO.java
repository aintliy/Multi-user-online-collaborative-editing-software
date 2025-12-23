package com.example.demo.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 文档版本视图对象
 */
@Data
public class DocumentVersionVO {
    
    private Long id;
    
    private Long documentId;
    
    private Integer versionNo;
    
    private String content;
    
    private String commitMessage;
    
    private Long createdBy;
    
    private String creatorName;
    
    private OffsetDateTime createdAt;
}
