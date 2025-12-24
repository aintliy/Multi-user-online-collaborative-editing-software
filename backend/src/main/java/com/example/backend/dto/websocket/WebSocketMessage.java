package com.example.backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket消息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    /**
     * 消息类型: EDIT, CURSOR, SELECTION, CHAT, NOTIFICATION, JOIN, LEAVE
     */
    private String type;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 发送者用户ID
     */
    private Long userId;
    
    /**
     * 发送者昵称
     */
    private String nickname;
    
    /**
     * 消息内容/数据
     */
    private Object data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}
