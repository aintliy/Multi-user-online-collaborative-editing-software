package com.example.demo.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 创建文档请求DTO
 */
@Data
public class CreateDocumentRequest {
    
    @NotBlank(message = "文档标题不能为空")
    private String title;
    
    /**
     * 文档类型：doc / sheet / slide / markdown
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
