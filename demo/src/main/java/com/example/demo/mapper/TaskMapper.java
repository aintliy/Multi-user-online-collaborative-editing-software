package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务Mapper接口
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
