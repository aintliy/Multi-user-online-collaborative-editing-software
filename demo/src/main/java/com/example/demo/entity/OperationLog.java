package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_logs")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String action;      // CREATE_DOC / DELETE_DOC / UPDATE_PERMISSION 等

    private String targetType;  // DOC / USER / ROLE / PERMISSION

    private Long targetId;

    private String detail;      // 详细信息

    private LocalDateTime createdAt;
}
