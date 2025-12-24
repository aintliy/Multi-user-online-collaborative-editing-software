package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.comment.CommentDTO;
import com.example.backend.dto.comment.CreateCommentRequest;
import com.example.backend.dto.comment.UpdateCommentRequest;
import com.example.backend.entity.User;
import com.example.backend.service.CommentService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 */
@RestController
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    private final UserService userService;
    
    /**
     * 创建评论
     */
    @PostMapping("/api/documents/{documentId}/comments")
    public ApiResponse<CommentDTO> createComment(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable Long documentId,
                                                  @Valid @RequestBody CreateCommentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        CommentDTO comment = commentService.createComment(documentId, user.getId(), request);
        return ApiResponse.success("评论成功", comment);
    }
    
    /**
     * 获取文档评论列表
     */
    @GetMapping("/api/documents/{documentId}/comments")
    public ApiResponse<List<CommentDTO>> getComments(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long documentId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<CommentDTO> comments = commentService.getComments(documentId, user.getId());
        return ApiResponse.success(comments);
    }
    
    /**
     * 更新评论状态
     */
    @PutMapping("/api/comments/{commentId}")
    public ApiResponse<CommentDTO> updateComment(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable Long commentId,
                                                  @RequestBody UpdateCommentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        CommentDTO comment = commentService.updateComment(commentId, user.getId(), request);
        return ApiResponse.success("更新成功", comment);
    }
}
