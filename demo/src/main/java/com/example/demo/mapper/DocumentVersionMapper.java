package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.DocumentVersion;

/**
 * 文档版本Mapper接口
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
}
