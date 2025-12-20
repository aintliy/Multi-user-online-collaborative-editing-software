package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文档请求 DTO
 */
@Data
public class CreateDocumentRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255, message = "标题长度不能超过 255 字符")
    private String title;

    private String content;

    private String docType;  // doc / sheet / slide，默认 doc
}
