package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.RolePermission;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
