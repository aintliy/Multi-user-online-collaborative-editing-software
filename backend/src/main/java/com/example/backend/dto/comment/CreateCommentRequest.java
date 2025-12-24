package com.example.backend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建评论请求DTO
 */
@Data
public class CreateCommentRequest {
    
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    private String rangeInfo;
    
    private Long replyToCommentId;
}
