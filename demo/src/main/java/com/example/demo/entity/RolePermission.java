package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("role_permissions")
public class RolePermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long roleId;
    
    private Long permissionId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
