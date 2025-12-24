package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务实体类
 * 对应数据库表 tasks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tasks")
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的文档
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    /**
     * 任务创建者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    /**
     * 任务分配人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;
    
    /**
     * 任务标题
     */
    @Column(nullable = false, length = 255)
    private String title;
    
    /**
     * 任务描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 任务状态: todo / doing / done
     */
    @Column(length = 20)
    @Builder.Default
    private String status = "todo";
    
    /**
     * 优先级: LOW / MEDIUM / HIGH
     */
    @Column(length = 20)
    @Builder.Default
    private String priority = "MEDIUM";
    
    /**
     * 截止日期
     */
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
