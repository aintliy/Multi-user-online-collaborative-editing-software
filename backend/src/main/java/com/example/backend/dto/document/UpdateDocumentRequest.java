package com.example.backend.dto.document;

import lombok.Data;

/**
 * 更新文档请求DTO
 */
@Data
public class UpdateDocumentRequest {
    
    private String title;
    private String visibility;
    private String tags;
}
