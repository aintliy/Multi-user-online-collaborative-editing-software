package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.DocumentPermission;

/**
 * 文档权限 Mapper
 */
@Mapper
public interface DocumentPermissionMapper extends BaseMapper<DocumentPermission> {
}
