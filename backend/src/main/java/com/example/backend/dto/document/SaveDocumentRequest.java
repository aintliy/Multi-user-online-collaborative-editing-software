package com.example.backend.dto.document;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存到 Redis confirmed 的请求体。
 */
@Data
public class SaveDocumentRequest {

    @NotBlank(message = "内容不能为空")
    private String content;
}
