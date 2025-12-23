package com.example.demo.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 用户实体（重构版）
 * 对应数据库表：users
 */
@Data
@TableName("users")
public class User {
    
    /**
     * 用户ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 对外公开的随机不可变用户ID，用于搜索和仓库URL
     * 示例：u_9f3a2c7b
     */
    @TableField("public_id")
    private String publicId;
    
    /**
     * 用户名
     */
    @TableField("username")
    private String username;
    
    /**
     * 邮箱（唯一）
     */
    @TableField("email")
    private String email;
    
    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;
    
    /**
     * 密码（BCrypt加密）
     */
    @TableField("password")
    private String password;
    
    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;
    
    /**
     * 个人简介
     */
    @TableField("profile")
    private String profile;
    
    /**
     * 用户状态：active-正常，disabled-禁用
     */
    @TableField("status")
    private String status;
    
    /**
     * 系统角色：ADMIN-管理员，USER-普通用户
     */
    @TableField("role") 
    private String role;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
