package com.example.backend.dto.document;

import lombok.Data;

/**
 * 保存到 Redis confirmed 的请求体。
 */
@Data
public class SaveDocumentRequest {

    private String content;
}
