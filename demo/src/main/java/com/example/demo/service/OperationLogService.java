package com.example.demo.service;

import com.example.demo.entity.OperationLog;
import com.example.demo.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {
    
    private final OperationLogMapper operationLogMapper;
    
    /**
     * 记录操作日志
     */
    public void log(Long userId, String action, String targetType, Long targetId, String detail) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(userId);
            operationLog.setAction(action);
            operationLog.setTargetType(targetType);
            operationLog.setTargetId(targetId);
            operationLog.setDetail(detail);
            operationLog.setCreatedAt(LocalDateTime.now());
            
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            // 日志记录失败不影响业务
            log.error("记录操作日志失败", e);
        }
    }
}
