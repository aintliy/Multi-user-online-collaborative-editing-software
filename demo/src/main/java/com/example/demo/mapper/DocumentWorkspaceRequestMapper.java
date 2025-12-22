package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.DocumentWorkspaceRequest;

/**
 * 文档协作申请Mapper接口
 */
@Mapper
public interface DocumentWorkspaceRequestMapper extends BaseMapper<DocumentWorkspaceRequest> {
}
