package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 操作日志实体（审计与追踪）
 * 对应数据库表：operation_logs
 */
@Data
@TableName("operation_logs")
public class OperationLog {
    
    /**
     * 日志ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 操作类型：CREATE_DOC-创建文档，DELETE_DOC-删除文档，UPDATE_PERMISSION-更新权限等
     */
    private String action;
    
    /**
     * 目标类型：DOC / USER / ROLE / PERMISSION
     */
    @TableField("target_type")
    private String targetType;
    
    /**
     * 目标ID
     */
    @TableField("target_id")
    private Long targetId;
    
    /**
     * 操作详情
     */
    private String detail;
    
    /**
     * 操作IP地址
     */
    @TableField("ip_address")
    private String ipAddress;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
