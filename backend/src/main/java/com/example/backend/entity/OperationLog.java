package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 对应数据库表 operation_logs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 操作用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * 操作类型: CREATE_DOC / DELETE_DOC / UPDATE_PERMISSION 等
     */
    @Column(length = 50, nullable = false)
    private String action;
    
    /**
     * 目标类型: DOC / USER / ROLE / PERMISSION
     */
    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;
    
    /**
     * 目标ID
     */
    @Column(name = "target_id")
    private Long targetId;
    
    /**
     * 详细信息
     */
    @Column(columnDefinition = "TEXT")
    private String detail;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
