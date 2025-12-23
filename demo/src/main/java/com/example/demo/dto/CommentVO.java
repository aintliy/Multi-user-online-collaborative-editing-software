package com.example.demo.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;
/**
 * 评论视图对象
 */
@Data
public class CommentVO {
    private Long id;
    private Long documentId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String content;
    private Long replyToCommentId;
    private String rangeInfo;
    private String status; // open / resolved
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<CommentVO> replies;
}