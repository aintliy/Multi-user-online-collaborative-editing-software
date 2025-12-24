package com.example.backend.dto.comment;

import com.example.backend.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    
    private Long id;
    private Long documentId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String content;
    private String rangeInfo;
    private Long replyToCommentId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static CommentDTO fromEntity(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .documentId(comment.getDocument().getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .avatarUrl(comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .rangeInfo(comment.getRangeInfo())
                .replyToCommentId(comment.getReplyToComment() != null ? comment.getReplyToComment().getId() : null)
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
