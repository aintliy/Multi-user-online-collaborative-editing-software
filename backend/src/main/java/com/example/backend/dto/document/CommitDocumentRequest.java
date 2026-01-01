package com.example.backend.dto.document;

import lombok.Data;

/**
 * 提交文档版本请求DTO
 */
@Data
public class CommitDocumentRequest {
    
    private String content;
    
    private String commitMessage;
}
