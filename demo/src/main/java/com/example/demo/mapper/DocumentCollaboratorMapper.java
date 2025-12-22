package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.DocumentCollaborator;

/**
 * 文档协作者Mapper接口
 */
@Mapper
public interface DocumentCollaboratorMapper extends BaseMapper<DocumentCollaborator> {
}
