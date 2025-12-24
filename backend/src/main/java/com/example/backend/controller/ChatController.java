package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.User;
import com.example.backend.service.ChatService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 聊天控制器
 */
@RestController
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final UserService userService;
    
    /**
     * 获取聊天历史
     */
    @GetMapping("/api/documents/{documentId}/chat-messages")
    public ApiResponse<PageResponse<Map<String, Object>>> getChatHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        PageResponse<Map<String, Object>> response = chatService.getChatHistory(documentId, user.getId(), page, pageSize);
        return ApiResponse.success(response);
    }
}
