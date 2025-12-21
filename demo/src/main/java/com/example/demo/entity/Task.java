package com.example.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tasks")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private Long creatorId;
    
    private Long assigneeId;
    
    private String title;
    
    private String description;
    
    /**
     * 任务状态：todo / doing / done
     */
    private String status;
    
    private LocalDate dueDate;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
