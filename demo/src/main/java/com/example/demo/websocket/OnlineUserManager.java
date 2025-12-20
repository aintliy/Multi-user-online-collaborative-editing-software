package com.example.demo.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.websocket.dto.OnlineUser;

/**
 * 在线用户管理器
 * 管理文档的在线用户列表
 */
@Component
public class OnlineUserManager {

    @Autowired
    private UserMapper userMapper;

    /**
     * 文档在线用户映射
     * Key: documentId
     * Value: Map<userId, OnlineUser>
     */
    private final Map<Long, Map<Long, OnlineUser>> documentUsers = new ConcurrentHashMap<>();

    /**
     * 预定义的光标颜色列表
     */
    private static final String[] CURSOR_COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A",
            "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E2"
    };

    /**
     * 用户加入文档
     */
    public List<OnlineUser> joinDocument(Long documentId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        documentUsers.putIfAbsent(documentId, new ConcurrentHashMap<>());
        Map<Long, OnlineUser> users = documentUsers.get(documentId);

        // 分配光标颜色
        String color = CURSOR_COLORS[users.size() % CURSOR_COLORS.length];

        OnlineUser onlineUser = new OnlineUser(
                userId,
                user.getUsername(),
                user.getAvatarUrl(),
                color,
                System.currentTimeMillis()
        );

        users.put(userId, onlineUser);

        return getOnlineUsers(documentId);
    }

    /**
     * 用户离开文档
     */
    public List<OnlineUser> leaveDocument(Long documentId, Long userId) {
        Map<Long, OnlineUser> users = documentUsers.get(documentId);
        if (users != null) {
            users.remove(userId);
            
            // 如果文档没有在线用户了，清理映射
            if (users.isEmpty()) {
                documentUsers.remove(documentId);
            }
        }

        return getOnlineUsers(documentId);
    }

    /**
     * 获取文档的在线用户列表
     */
    public List<OnlineUser> getOnlineUsers(Long documentId) {
        Map<Long, OnlineUser> users = documentUsers.get(documentId);
        if (users == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(users.values());
    }

    /**
     * 获取用户的光标颜色
     */
    public String getUserColor(Long documentId, Long userId) {
        Map<Long, OnlineUser> users = documentUsers.get(documentId);
        if (users != null) {
            OnlineUser user = users.get(userId);
            if (user != null) {
                return user.getCursorColor();
            }
        }
        return CURSOR_COLORS[0];
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long documentId, Long userId) {
        Map<Long, OnlineUser> users = documentUsers.get(documentId);
        return users != null && users.containsKey(userId);
    }

    /**
     * 获取文档的在线用户数
     */
    public int getOnlineUserCount(Long documentId) {
        Map<Long, OnlineUser> users = documentUsers.get(documentId);
        return users != null ? users.size() : 0;
    }
}
