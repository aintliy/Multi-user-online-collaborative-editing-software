package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建评论请求 DTO
 */
@Data
public class CreateCommentRequest {

    @NotNull(message = "文档 ID 不能为空")
    private Long documentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long replyToCommentId;  // 回复的评论 ID（可选）

    private String rangeInfo;  // 选中范围信息（JSON 格式）
}
