package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Document;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
