package com.example.demo.dto;

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
     * 文档类型：markdown / docx / txt
     */
    private String type;
    
    /**
     * 可见性：private / public
     */
    private String visibility;
    
    /**
     * 模板ID（可选）
     */
    private Long templateId;
}
