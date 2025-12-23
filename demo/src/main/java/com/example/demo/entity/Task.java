package com.example.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 任务实体（基于文档的任务分配与跟踪）
 * 对应数据库表：tasks
 */
@Data
@TableName("tasks")
public class Task {
    
    /**
     * 任务ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;
    
    /**
     * 创建人ID
     */
    @TableField("creator_id")
    private Long creatorId;
    
    /**
     * 指派人ID
     */
    @TableField("assignee_id")
    private Long assigneeId;
    
    /**
     * 任务标题
     */
    @TableField("title")
    private String title;
    
    /**
     * 任务描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 任务状态：TODO-待办，DOING-进行中，DONE-已完成
     */
    @TableField("status")
    private String status;
    
    /**
     * 任务优先级：LOW-低，MEDIUM-中，HIGH-高
     */
    private String priority;   
    
    /**
     * 关联文档ID
     */
    @TableField("related_doc_id")
    private Long relatedDocId;
    
    /**
     * 截止日期
     */
    @TableField("due_date")
    private LocalDate dueDate;
    
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
