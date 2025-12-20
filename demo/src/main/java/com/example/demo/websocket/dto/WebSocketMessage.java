package com.example.demo.websocket.dto;

import lombok.Data;

/**
 * WebSocket 消息基类
 */
@Data
public class WebSocketMessage {

    /**
     * 消息类型
     * EDIT - 编辑操作
     * CURSOR - 光标位置
     * COMMENT - 评论
     * CHAT - 聊天
     * JOIN - 加入文档
     * LEAVE - 离开文档
     * ONLINE_USERS - 在线用户列表
     */
    private String type;

    /**
     * 文档 ID
     */
    private Long docId;

    /**
     * 发送者用户 ID
     */
    private Long userId;

    /**
     * 发送者用户名
     */
    private String username;

    /**
     * 消息内容（JSON 格式）
     */
    private Object payload;

    /**
     * 时间戳
     */
    private Long timestamp;

    public WebSocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public WebSocketMessage(String type, Long docId, Long userId, String username, Object payload) {
        this.type = type;
        this.docId = docId;
        this.userId = userId;
        this.username = username;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
}
