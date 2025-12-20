package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新文档请求 DTO
 */
@Data
public class UpdateDocumentRequest {

    @Size(max = 255, message = "标题长度不能超过 255 字符")
    private String title;

    private String content;
}
