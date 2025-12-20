package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CommentVO;
import com.example.demo.dto.CreateCommentRequest;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Document;
import com.example.demo.entity.User;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务
 */
@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 创建评论
     */
    @Transactional
    public CommentVO createComment(Long userId, CreateCommentRequest request) {
        // 检查文档是否存在和权限
        Document document = documentMapper.selectById(request.getDocumentId());
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 检查权限（必须有访问权限才能评论）
        try {
            documentService.getDocumentById(userId, request.getDocumentId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION, "无权在该文档评论");
        }

        // 如果是回复评论，检查父评论是否存在
        if (request.getReplyToCommentId() != null) {
            Comment parentComment = commentMapper.selectById(request.getReplyToCommentId());
            if (parentComment == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "回复的评论不存在");
            }
        }

        // 创建评论
        Comment comment = new Comment();
        comment.setDocumentId(request.getDocumentId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setReplyToCommentId(request.getReplyToCommentId());
        comment.setRangeInfo(request.getRangeInfo());
        comment.setStatus("open");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        commentMapper.insert(comment);

        // 发送通知给文档所有者（如果不是自己评论自己的文档）
        if (!document.getOwnerId().equals(userId)) {
            notificationService.createNotification(
                    document.getOwnerId(),
                    "COMMENT",
                    comment.getId(),
                    "用户在您的文档《" + document.getTitle() + "》中添加了评论"
            );
        }

        // 如果是回复，通知被回复的用户
        if (request.getReplyToCommentId() != null) {
            Comment parentComment = commentMapper.selectById(request.getReplyToCommentId());
            if (parentComment != null && !parentComment.getUserId().equals(userId)) {
                notificationService.createNotification(
                        parentComment.getUserId(),
                        "COMMENT",
                        comment.getId(),
                        "用户回复了您的评论"
                );
            }
        }

        return convertToVO(comment);
    }

    /**
     * 获取文档的评论列表
     */
    public List<CommentVO> getCommentsByDocument(Long userId, Long documentId) {
        // 检查权限
        documentService.getDocumentById(userId, documentId);

        // 查询所有评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDocumentId, documentId)
               .orderByDesc(Comment::getCreatedAt);

        List<Comment> comments = commentMapper.selectList(wrapper);

        // 转换为 VO 并构建树形结构
        List<CommentVO> commentVOs = comments.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建评论树（顶级评论 + 回复）
        Map<Long, List<CommentVO>> replyMap = commentVOs.stream()
                .filter(vo -> vo.getReplyToCommentId() != null)
                .collect(Collectors.groupingBy(CommentVO::getReplyToCommentId));

        List<CommentVO> topComments = commentVOs.stream()
                .filter(vo -> vo.getReplyToCommentId() == null)
                .collect(Collectors.toList());

        // 设置回复
        for (CommentVO comment : topComments) {
            comment.setReplies(replyMap.getOrDefault(comment.getId(), new ArrayList<>()));
        }

        return topComments;
    }

    /**
     * 解决评论（标记为已解决）
     */
    public void resolveComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在");
        }

        // 检查权限（文档所有者或评论创建者可以解决评论）
        Document document = documentMapper.selectById(comment.getDocumentId());
        if (!document.getOwnerId().equals(userId) && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权解决该评论");
        }

        comment.setStatus("resolved");
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    /**
     * 删除评论
     */
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在");
        }

        // 检查权限（只有评论创建者或文档所有者可以删除）
        Document document = documentMapper.selectById(comment.getDocumentId());
        if (!comment.getUserId().equals(userId) && !document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该评论");
        }

        commentMapper.deleteById(commentId);

        // 删除该评论的所有回复
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getReplyToCommentId, commentId);
        commentMapper.delete(wrapper);
    }

    /**
     * 实体转 VO
     */
    private CommentVO convertToVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        // 设置用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setUserAvatar(user.getAvatarUrl());
        }

        // 如果是回复，设置被回复的用户名
        if (comment.getReplyToCommentId() != null) {
            Comment parentComment = commentMapper.selectById(comment.getReplyToCommentId());
            if (parentComment != null) {
                User parentUser = userMapper.selectById(parentComment.getUserId());
                if (parentUser != null) {
                    vo.setReplyToUsername(parentUser.getUsername());
                }
            }
        }

        return vo;
    }
}
