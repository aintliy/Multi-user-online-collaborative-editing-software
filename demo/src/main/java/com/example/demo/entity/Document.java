package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档实体
 */
@Data
@TableName("documents")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Long ownerId;

    private String content;

    private String docType;  // doc / sheet / slide

    private String status;   // active / deleted

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
