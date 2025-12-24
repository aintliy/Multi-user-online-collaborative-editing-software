package com.example.backend.dto.document;

import lombok.Data;

/**
 * 移动文档请求DTO
 */
@Data
public class MoveDocumentRequest {
    
    private Long targetFolderId;
}
