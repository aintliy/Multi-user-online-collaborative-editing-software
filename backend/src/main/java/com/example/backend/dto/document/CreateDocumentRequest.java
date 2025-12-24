package com.example.backend.dto.document;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建文档请求DTO
 */
@Data
public class CreateDocumentRequest {
    
    @NotBlank(message = "文档标题不能为空")
    private String title;
    
    /**
     * 文档类型: markdown / docx / txt / sheet / slide
     */
    private String type = "markdown";
    
    /**
     * 可见性: private / public
     */
    private String visibility = "private";
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 文件夹ID
     */
    private Long folderId;
}
