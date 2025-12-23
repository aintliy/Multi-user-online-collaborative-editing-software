package com.example.demo.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CommentVO;
import com.example.demo.dto.CreateCommentRequest;
import com.example.demo.dto.DocumentVO;
import com.example.demo.dto.PageResponse;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Document;
import com.example.demo.entity.User;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 评论服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentMapper commentMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    
    /**
     * 创建评论
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(Long userId, CreateCommentRequest request) {
        Document document = documentMapper.selectById(request.getDocumentId());
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查读权限
        DocumentVO docVO = documentService.getDocumentById(request.getDocumentId(), userId);
        if (docVO == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        // 如果是回复，检查父评论是否存在
        if (request.getReplyToCommentId() != null) {
            Comment parentComment = commentMapper.selectById(request.getReplyToCommentId());
            if (parentComment == null) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            }
        }
        
        Comment comment = new Comment();
        comment.setDocumentId(request.getDocumentId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setReplyToCommentId(request.getReplyToCommentId());
        comment.setStatus("open");
        comment.setCreatedAt(OffsetDateTime.now());
        comment.setUpdatedAt(OffsetDateTime.now());
        
        commentMapper.insert(comment);
        
        // 如果是回复，通知被回复的用户
        if (request.getReplyToCommentId() != null) {
            Comment parentComment = commentMapper.selectById(request.getReplyToCommentId());
            if (parentComment != null && !parentComment.getUserId().equals(userId)) {
                notificationService.createNotification(
                    parentComment.getUserId(),
                    "COMMENT_REPLY",
                    "有人回复了你的评论",
                    comment.getId()
                );
            }
        } else {
            // 如果是新评论，通知文档所有者
            if (!document.getOwnerId().equals(userId)) {
                notificationService.createNotification(
                    document.getOwnerId(),
                    "COMMENT_ADDED",
                    "文档 " + document.getTitle() + " 有新评论",
                    comment.getId()
                );
            }
        }
        
        // 记录操作日志
        operationLogService.log(userId, "CREATE_COMMENT", "DOC", request.getDocumentId(), 
            "添加评论");
        
        log.info("创建评论: commentId={}, docId={}, userId={}", 
            comment.getId(), request.getDocumentId(), userId);
        
        return convertToCommentVO(comment);
    }
    
    /**
     * 获取文档评论列表
     */
    public PageResponse<CommentVO> getDocumentComments(Long documentId, Long userId, 
                                                        Integer page, Integer pageSize) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查读权限
        DocumentVO docVO = documentService.getDocumentById(documentId, userId);
        if (docVO == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        Page<Comment> pageObj = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDocumentId, documentId);
        wrapper.isNull(Comment::getReplyToCommentId); // 只获取顶级评论
        wrapper.orderByDesc(Comment::getCreatedAt);
        
        IPage<Comment> result = commentMapper.selectPage(pageObj, wrapper);
        
        List<CommentVO> items = result.getRecords().stream()
            .map(comment -> {
                CommentVO vo = convertToCommentVO(comment);
                // 加载回复
                vo.setReplies(getReplies(comment.getId()));
                return vo;
            })
            .collect(Collectors.toList());
        
        return new PageResponse<>(items, page, pageSize, result.getTotal());
    }
    
    /**
     * 获取评论的回复
     */
    private List<CommentVO> getReplies(Long parentId) {
        List<Comment> replies = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getReplyToCommentId, parentId)
                .orderByAsc(Comment::getCreatedAt)
        );
        
        return replies.stream()
            .map(this::convertToCommentVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 删除评论
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        
        // 只有评论作者或文档所有者可以删除
        Document document = documentMapper.selectById(comment.getDocumentId());
        if (!comment.getUserId().equals(userId) && !document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_NO_PERMISSION);
        }
        
        // 删除评论及其所有回复
        commentMapper.deleteById(commentId);
        commentMapper.delete(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getReplyToCommentId, commentId)
        );
        
        // 记录操作日志
        operationLogService.log(userId, "DELETE_COMMENT", "DOC", comment.getDocumentId(), 
            "删除评论");
        
        log.info("删除评论: commentId={}, userId={}", commentId, userId);
    }
    
    /**
     * 标记评论已解决
     */
    @Transactional(rollbackFor = Exception.class)
    public void resolveComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        
        Document document = documentMapper.selectById(comment.getDocumentId());
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有文档所有者可以标记评论已解决
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        comment.setStatus("resolved");
        comment.setUpdatedAt(OffsetDateTime.now());
        commentMapper.updateById(comment);
        
        // 通知评论作者
        if (!comment.getUserId().equals(userId)) {
            notificationService.createNotification(
                comment.getUserId(),
                "COMMENT_RESOLVED",
                "你的评论已被标记为已解决",
                commentId
            );
        }
        
        // 记录操作日志
        operationLogService.log(userId, "RESOLVE_COMMENT", "DOC", comment.getDocumentId(), 
            "标记评论已解决");
        
        log.info("标记评论已解决: commentId={}, userId={}", commentId, userId);
    }
    
    /**
     * 转换为CommentVO
     */
    private CommentVO convertToCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setDocumentId(comment.getDocumentId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setReplyToCommentId(comment.getReplyToCommentId());
        vo.setRangeInfo(comment.getRangeInfo());
        vo.setStatus(comment.getStatus());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setUpdatedAt(comment.getUpdatedAt());
        
        // 设置用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setAvatarUrl(user.getAvatarUrl());
        }
        
        return vo;
    }
}
