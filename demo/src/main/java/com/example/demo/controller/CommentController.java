package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Result;
import com.example.demo.dto.CommentVO;
import com.example.demo.dto.CreateCommentRequest;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * 创建评论
     */
    @PostMapping
    public Result<CommentVO> createComment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentVO comment = commentService.createComment(userId, request);
        return Result.success(comment);
    }
    
    /**
     * 获取文档评论列表
     */
    @GetMapping("/document/{documentId}")
    public Result<PageResponse<CommentVO>> getDocumentComments(
            @PathVariable Long documentId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResponse<CommentVO> comments = commentService.getDocumentComments(
                documentId, userId, page, pageSize);
        return Result.success(comments);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        commentService.deleteComment(id, userId);
        return Result.success();
    }
    
    /**
     * 标记评论已解决
     */
    @PutMapping("/{id}/resolve")
    public Result<Void> resolveComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        commentService.resolveComment(id, userId);
        return Result.success();
    }
}
