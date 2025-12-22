package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * 提交文档版本请求DTO
 */
@Data
public class CommitDocumentRequest {
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private String commitMessage;
}
