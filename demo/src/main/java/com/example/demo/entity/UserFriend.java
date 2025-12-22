package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 好友关系实体
 * 对应数据库表：user_friends
 */
@Data
@TableName("user_friends")
public class UserFriend {
    
    /**
     * 好友关系ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 好友用户ID
     */
    @TableField("friend_id")
    private Long friendId;
    
    /**
     * 关系状态：PENDING-待处理，ACCEPTED-已接受，REJECTED-已拒绝，BLOCKED-已屏蔽
     */
    private String status;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
