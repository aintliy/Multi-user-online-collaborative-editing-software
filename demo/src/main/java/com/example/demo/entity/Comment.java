package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 评论实体
 */
@Data
@TableName("comments")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long userId;

    private String content;

    private Long replyToCommentId;

    private String rangeInfo;  // JSON 字符串，记录选中范围

    private String status;  // open / resolved

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
