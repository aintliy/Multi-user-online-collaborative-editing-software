package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表 users
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 对外展示与搜索使用的随机不可变ID
     * 例如 "u_9f3a2c7b"
     */
    @Column(name = "public_id", nullable = false, unique = true, length = 32)
    private String publicId;
    
    @Column(nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;
    
    @Column(columnDefinition = "TEXT")
    private String profile;
    
    /**
     * 用户状态: ACTIVE / disabled
     */
    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    
    /**
     * 系统角色: ADMIN / USER
     */
    @Column(length = 20)
    @Builder.Default
    private String role = "USER";
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
