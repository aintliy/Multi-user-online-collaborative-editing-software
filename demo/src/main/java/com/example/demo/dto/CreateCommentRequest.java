package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * 创建评论请求DTO
 */
@Data
public class CreateCommentRequest {
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    @NotBlank(message = "评论内容不能为空")
    private String content;
    /**
     * 选中范围信息（JSON格式）
     */
    private String rangeInfo;
    /**
     * 回复目标评论ID
     */
    private Long replyToCommentId;
}