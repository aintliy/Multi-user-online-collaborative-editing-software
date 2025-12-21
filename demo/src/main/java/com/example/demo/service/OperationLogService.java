package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.OperationLog;
import com.example.demo.mapper.OperationLogMapper;

/**
 * 操作日志服务
 */
@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 记录操作日志
     */
    public void log(Long userId, String action, String targetType, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        
        operationLogMapper.insert(log);
    }

    /**
     * 获取操作日志列表（分页，支持筛选）
     */
    public IPage<OperationLog> getOperationLogs(int pageNum, int pageSize, 
                                                  Long userId, String action, 
                                                  String targetType, String startDate, String endDate) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 按用户筛选
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        
        // 按操作类型筛选
        if (action != null && !action.isEmpty()) {
            wrapper.eq(OperationLog::getAction, action);
        }
        
        // 按目标类型筛选
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(OperationLog::getTargetType, targetType);
        }
        
        // 日期范围筛选
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(OperationLog::getCreatedAt, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(OperationLog::getCreatedAt, endDate);
        }
        
        wrapper.orderByDesc(OperationLog::getCreatedAt);

        return operationLogMapper.selectPage(page, wrapper);
    }

    /**
     * 获取用户的操作历史
     */
    public List<OperationLog> getUserOperationHistory(Long userId, int limit) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getUserId, userId)
               .orderByDesc(OperationLog::getCreatedAt)
               .last("LIMIT " + limit);
        
        return operationLogMapper.selectList(wrapper);
    }
}
