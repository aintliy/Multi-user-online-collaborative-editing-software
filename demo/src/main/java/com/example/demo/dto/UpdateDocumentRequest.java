package com.example.demo.dto;

import lombok.Data;

/**
 * 更新文档请求DTO
 */
@Data
public class UpdateDocumentRequest {
    
    private String title;
    
    /**
     * 可见性：private / public
     */
    private String visibility;
    
    private String tags;
}
