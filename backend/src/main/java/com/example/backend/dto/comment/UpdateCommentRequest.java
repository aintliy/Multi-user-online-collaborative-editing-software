package com.example.backend.dto.comment;

import lombok.Data;

/**
 * 更新评论请求DTO
 */
@Data
public class UpdateCommentRequest {
    
    private String status; // open / resolved
}
