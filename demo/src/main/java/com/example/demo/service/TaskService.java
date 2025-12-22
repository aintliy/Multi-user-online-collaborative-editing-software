package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CreateTaskRequest;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.TaskVO;
import com.example.demo.dto.UpdateTaskRequest;
import com.example.demo.entity.Task;
import com.example.demo.entity.User;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    
    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskVO createTask(Long userId, CreateTaskRequest request) {
        // 检查受让人是否存在
        if (request.getAssigneeId() != null) {
            User assignee = userMapper.selectById(request.getAssigneeId());
            if (assignee == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
        }
        
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCreatorId(userId);
        task.setAssigneeId(request.getAssigneeId());
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setStatus("TODO");
        task.setDueDate(request.getDueDate());
        task.setRelatedDocId(request.getRelatedDocId());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        taskMapper.insert(task);
        
        // 如果指定了受让人，发送通知
        if (request.getAssigneeId() != null && !request.getAssigneeId().equals(userId)) {
            notificationService.createNotification(
                request.getAssigneeId(),
                "TASK_ASSIGNED",
                "你有一个新任务: " + task.getTitle(),
                task.getId()
            );
        }
        
        // 记录操作日志
        operationLogService.log(userId, "CREATE_TASK", "TASK", task.getId(), 
            "创建任务: " + task.getTitle());
        
        log.info("创建任务: taskId={}, userId={}, title={}", task.getId(), userId, task.getTitle());
        
        return convertToTaskVO(task);
    }
    
    /**
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskVO updateTask(Long taskId, Long userId, UpdateTaskRequest request) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        
        // 只有创建者或受让人可以更新任务
        if (!task.getCreatorId().equals(userId) && 
            (task.getAssigneeId() == null || !task.getAssigneeId().equals(userId))) {
            throw new BusinessException(ErrorCode.TASK_NO_PERMISSION);
        }
        
        boolean statusChanged = false;
        String oldStatus = task.getStatus();
        
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            statusChanged = !oldStatus.equals(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssigneeId() != null && !request.getAssigneeId().equals(task.getAssigneeId())) {
            User assignee = userMapper.selectById(request.getAssigneeId());
            if (assignee == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            task.setAssigneeId(request.getAssigneeId());
            
            // 发送通知
            notificationService.createNotification(
                request.getAssigneeId(),
                "TASK_ASSIGNED",
                "任务 " + task.getTitle() + " 已分配给你",
                taskId
            );
        }
        
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        
        // 如果状态改变，通知相关人员
        if (statusChanged) {
            // 通知创建者
            if (!task.getCreatorId().equals(userId)) {
                notificationService.createNotification(
                    task.getCreatorId(),
                    "TASK_STATUS_CHANGED",
                    "任务 " + task.getTitle() + " 状态已更新为: " + task.getStatus(),
                    taskId
                );
            }
            // 通知受让人
            if (task.getAssigneeId() != null && !task.getAssigneeId().equals(userId)) {
                notificationService.createNotification(
                    task.getAssigneeId(),
                    "TASK_STATUS_CHANGED",
                    "任务 " + task.getTitle() + " 状态已更新为: " + task.getStatus(),
                    taskId
                );
            }
        }
        
        // 记录操作日志
        operationLogService.log(userId, "UPDATE_TASK", "TASK", taskId, 
            "更新任务: " + task.getTitle());
        
        log.info("更新任务: taskId={}, userId={}", taskId, userId);
        
        return convertToTaskVO(task);
    }
    
    /**
     * 删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        
        // 只有创建者可以删除任务
        if (!task.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.TASK_NO_PERMISSION);
        }
        
        taskMapper.deleteById(taskId);
        
        // 记录操作日志
        operationLogService.log(userId, "DELETE_TASK", "TASK", taskId, 
            "删除任务: " + task.getTitle());
        
        log.info("删除任务: taskId={}, userId={}", taskId, userId);
    }
    
    /**
     * 获取任务列表
     */
    public PageResponse<TaskVO> getTasks(Long userId, Integer page, Integer pageSize, 
                                          String status, String priority, Long assigneeId, 
                                          Long creatorId, Long relatedDocId) {
        Page<Task> pageObj = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        
        // 筛选条件
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Task::getStatus, status);
        }
        if (priority != null && !priority.trim().isEmpty()) {
            wrapper.eq(Task::getPriority, priority);
        }
        if (assigneeId != null) {
            wrapper.eq(Task::getAssigneeId, assigneeId);
        }
        if (creatorId != null) {
            wrapper.eq(Task::getCreatorId, creatorId);
        }
        if (relatedDocId != null) {
            wrapper.eq(Task::getRelatedDocId, relatedDocId);
        }
        
        // 默认查询与当前用户相关的任务
        if (assigneeId == null && creatorId == null) {
            wrapper.and(w -> w
                .eq(Task::getCreatorId, userId)
                .or()
                .eq(Task::getAssigneeId, userId)
            );
        }
        
        wrapper.orderByDesc(Task::getCreatedAt);
        
        IPage<Task> result = taskMapper.selectPage(pageObj, wrapper);
        
        List<TaskVO> items = result.getRecords().stream()
            .map(this::convertToTaskVO)
            .collect(Collectors.toList());
        
        return new PageResponse<>(items, page, pageSize, result.getTotal());
    }
    
    /**
     * 获取任务详情
     */
    public TaskVO getTaskById(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        
        // 只有创建者或受让人可以查看任务
        if (!task.getCreatorId().equals(userId) && 
            (task.getAssigneeId() == null || !task.getAssigneeId().equals(userId))) {
            throw new BusinessException(ErrorCode.TASK_NO_PERMISSION);
        }
        
        return convertToTaskVO(task);
    }
    
    /**
     * 转换为TaskVO
     */
    private TaskVO convertToTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setCreatorId(task.getCreatorId());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setStatus(task.getStatus());
        vo.setPriority(task.getPriority());
        vo.setDueDate(task.getDueDate());
        vo.setDocumentId(task.getRelatedDocId());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        
        // 设置创建者信息
        User creator = userMapper.selectById(task.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getUsername());
        }
        
        // 设置受让人信息
        if (task.getAssigneeId() != null) {
            User assignee = userMapper.selectById(task.getAssigneeId());
            if (assignee != null) {
                vo.setAssigneeName(assignee.getUsername());
            }
        }
        
        return vo;
    }
}
