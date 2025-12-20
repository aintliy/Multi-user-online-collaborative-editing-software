package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Result;
import com.example.demo.dto.CommentVO;
import com.example.demo.dto.CreateCommentRequest;
import com.example.demo.service.CommentService;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 创建评论
     * POST /api/comments
     */
    @PostMapping
    public Result<CommentVO> createComment(@Validated @RequestBody CreateCommentRequest request,
                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        CommentVO comment = commentService.createComment(userId, request);
        return Result.success("评论创建成功", comment);
    }

    /**
     * 获取文档的评论列表
     * GET /api/comments/document/{docId}
     */
    @GetMapping("/document/{docId}")
    public Result<List<CommentVO>> getCommentsByDocument(@PathVariable Long docId,
                                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<CommentVO> comments = commentService.getCommentsByDocument(userId, docId);
        return Result.success(comments);
    }

    /**
     * 解决评论
     * PUT /api/comments/{id}/resolve
     */
    @PutMapping("/{id}/resolve")
    public Result<Void> resolveComment(@PathVariable Long id,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        commentService.resolveComment(userId, id);
        return Result.success("评论已标记为已解决", null);
    }

    /**
     * 删除评论
     * DELETE /api/comments/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        commentService.deleteComment(userId, id);
        return Result.success("评论删除成功", null);
    }
}
