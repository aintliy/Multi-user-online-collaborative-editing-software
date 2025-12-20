package com.example.demo.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 在线用户信息
 */
@Data
@AllArgsConstructor
public class OnlineUser {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 光标颜色
     */
    private String cursorColor;

    /**
     * 加入时间
     */
    private Long joinTime;
}
