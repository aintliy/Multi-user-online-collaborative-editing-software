package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.CommentVO;
import com.example.demo.dto.CreateCommentRequest;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器（重构版）
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
    public ApiResponse<CommentVO> createComment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentVO comment = commentService.createComment(userId, request);
        return ApiResponse.success(comment);
    }
    
    /**
     * 获取文档评论列表
     */
    @GetMapping("/document/{documentId}")
    public ApiResponse<PageResponse<CommentVO>> getDocumentComments(
            @PathVariable Long documentId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResponse<CommentVO> comments = commentService.getDocumentComments(
                documentId, userId, page, pageSize);
        return ApiResponse.success(comments);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        commentService.deleteComment(id, userId);
        return ApiResponse.success();
    }
    
    /**
     * 标记评论已解决
     */
    @PutMapping("/{id}/resolve")
    public ApiResponse<Void> resolveComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        commentService.resolveComment(id, userId);
        return ApiResponse.success();
    }
}
