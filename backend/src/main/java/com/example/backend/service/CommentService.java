package com.example.backend.service;

import com.example.backend.dto.comment.CommentDTO;
import com.example.backend.dto.comment.CreateCommentRequest;
import com.example.backend.dto.comment.UpdateCommentRequest;
import com.example.backend.entity.Comment;
import com.example.backend.entity.Document;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务
 */
@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    private final UserService userService;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    
    /**
     * 创建评论
     */
    @Transactional
    public CommentDTO createComment(Long documentId, Long userId, CreateCommentRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        // 检查访问权限
        if (!documentService.checkDocumentAccess(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        User user = userService.getUserById(userId);
        
        Comment replyToComment = null;
        if (request.getReplyToCommentId() != null) {
            replyToComment = commentRepository.findById(request.getReplyToCommentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "回复的评论不存在"));
        }
        
        Comment comment = Comment.builder()
                .document(document)
                .user(user)
                .content(request.getContent())
                .rangeInfo(request.getRangeInfo())
                .replyToComment(replyToComment)
                .status("open")
                .build();
        
        comment = commentRepository.save(comment);
        
        // 通知文档所有者（如果不是自己评论自己的文档）
        if (!document.getOwner().getId().equals(userId)) {
            notificationService.createNotification(
                    document.getOwner().getId(),
                    "COMMENT",
                    documentId,
                    user.getUsername() + " 在文档「" + document.getTitle() + "」中添加了评论"
            );
        }
        
        // 如果是回复评论，通知被回复的用户
        if (replyToComment != null && !replyToComment.getUser().getId().equals(userId)) {
            notificationService.createNotification(
                    replyToComment.getUser().getId(),
                    "COMMENT_REPLY",
                    documentId,
                    user.getUsername() + " 回复了您的评论"
            );
        }
        
        return CommentDTO.fromEntity(comment);
    }
    
    /**
     * 获取文档评论列表
     */
    public List<CommentDTO> getComments(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        if (!documentService.checkDocumentAccess(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        return commentRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream()
                .map(CommentDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新评论状态
     */
    @Transactional
    public CommentDTO updateComment(Long commentId, Long userId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在"));
        
        // 只有评论者或文档所有者可以更新状态
        if (!comment.getUser().getId().equals(userId) && 
            !comment.getDocument().getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此评论");
        }
        
        if (request.getStatus() != null) {
            comment.setStatus(request.getStatus());
        }
        
        comment = commentRepository.save(comment);
        return CommentDTO.fromEntity(comment);
    }
}
