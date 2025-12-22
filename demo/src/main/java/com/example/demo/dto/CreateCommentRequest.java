package com.example.demo.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 创建评论请求DTO
 */
@Data
public class CreateCommentRequest {
    
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
