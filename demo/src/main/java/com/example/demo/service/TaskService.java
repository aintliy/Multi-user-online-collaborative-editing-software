package com.example.demo.service;

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
import com.example.demo.dto.TaskVO;
import com.example.demo.dto.UpdateTaskRequest;
import com.example.demo.entity.Document;
import com.example.demo.entity.DocumentPermission;
import com.example.demo.entity.Task;
import com.example.demo.entity.User;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.DocumentPermissionMapper;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskMapper taskMapper;
    private final DocumentMapper documentMapper;
    private final DocumentPermissionMapper documentPermissionMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    
    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskVO createTask(Long userId, Long documentId, CreateTaskRequest request) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(documentId);
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查权限：必须是文档所有者或编辑者
        if (!document.getOwnerId().equals(userId)) {
            DocumentPermission permission = documentPermissionMapper.selectOne(
                new LambdaQueryWrapper<DocumentPermission>()
                    .eq(DocumentPermission::getDocumentId, documentId)
                    .eq(DocumentPermission::getUserId, userId)
            );
            if (permission == null || !"EDITOR".equals(permission.getRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        
        // 检查被分配人是否存在
        User assignee = userMapper.selectById(request.getAssigneeId());
        if (assignee == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 创建任务
        Task task = new Task();
        task.setDocumentId(documentId);
        task.setCreatorId(userId);
        task.setAssigneeId(request.getAssigneeId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus("todo");
        task.setDueDate(request.getDueDate());
        
        taskMapper.insert(task);
        
        // 发送通知给被分配人
        if (!request.getAssigneeId().equals(userId)) {
            notificationService.createNotification(
                request.getAssigneeId(),
                "TASK",
                task.getId(),
                String.format("您有新任务：%s", task.getTitle())
            );
        }
        
        return convertToVO(task);
    }
    
    /**
     * 获取文档的任务列表
     */
    public List<TaskVO> getTasksByDocument(Long userId, Long documentId) {
        // 检查文档访问权限
        Document document = documentMapper.selectById(documentId);
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查权限
        if (!document.getOwnerId().equals(userId)) {
            DocumentPermission permission = documentPermissionMapper.selectOne(
                new LambdaQueryWrapper<DocumentPermission>()
                    .eq(DocumentPermission::getDocumentId, documentId)
                    .eq(DocumentPermission::getUserId, userId)
            );
            if (permission == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        
        // 查询任务列表
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getDocumentId, documentId)
                .orderByDesc(Task::getCreatedAt)
        );
        
        return tasks.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取用户的所有任务
     */
    public IPage<TaskVO> getMyTasks(Long userId, int page, int size, String status) {
        Page<Task> taskPage = new Page<>(page, size);
        
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
            .eq(Task::getAssigneeId, userId)
            .orderByDesc(Task::getCreatedAt);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Task::getStatus, status);
        }
        
        IPage<Task> result = taskMapper.selectPage(taskPage, wrapper);
        
        return result.convert(this::convertToVO);
    }
    
    /**
     * 获取用户的所有任务（列表形式）
     */
    public List<TaskVO> getMyTasksList(Long userId, String status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
            .eq(Task::getAssigneeId, userId)
            .orderByDesc(Task::getCreatedAt);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Task::getStatus, status);
        }
        
        List<Task> tasks = taskMapper.selectList(wrapper);
        
        return tasks.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取任务详情
     */
    public TaskVO getTaskById(Long userId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        
        // 只有任务负责人可以查看
        if (!task.getAssigneeId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        return convertToVO(task);
    }
    
    /**
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskVO updateTask(Long userId, Long taskId, UpdateTaskRequest request) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        
        // 只有任务创建者或被分配人可以更新任务
        if (!task.getCreatorId().equals(userId) && !task.getAssigneeId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // 更新字段
        boolean statusChanged = false;
        String oldStatus = task.getStatus();
        
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            statusChanged = !oldStatus.equals(request.getStatus());
        }
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getAssigneeId() != null) {
            task.setAssigneeId(request.getAssigneeId());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        
        taskMapper.updateById(task);
        
        // 如果状态变化，发送通知
        if (statusChanged) {
            Long notifyUserId = task.getCreatorId().equals(userId) 
                ? task.getAssigneeId() 
                : task.getCreatorId();
            
            if (!notifyUserId.equals(userId)) {
                String statusText = "todo".equals(task.getStatus()) ? "待处理" 
                    : "doing".equals(task.getStatus()) ? "进行中" : "已完成";
                
                notificationService.createNotification(
                    notifyUserId,
                    "TASK",
                    task.getId(),
                    String.format("任务 \"%s\" 状态更新为：%s", task.getTitle(), statusText)
                );
            }
        }
        
        return convertToVO(task);
    }
    
    /**
     * 删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long userId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        
        // 只有任务创建者可以删除
        if (!task.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        taskMapper.deleteById(taskId);
    }
    
    /**
     * 转换为 VO
     */
    private TaskVO convertToVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setDocumentId(task.getDocumentId());
        vo.setCreatorId(task.getCreatorId());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setStatus(task.getStatus());
        vo.setDueDate(task.getDueDate());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        
        // 填充文档信息
        Document document = documentMapper.selectById(task.getDocumentId());
        if (document != null) {
            vo.setDocumentTitle(document.getTitle());
        }
        
        // 填充创建者信息
        User creator = userMapper.selectById(task.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getUsername());
        }
        
        // 填充分配人信息
        User assignee = userMapper.selectById(task.getAssigneeId());
        if (assignee != null) {
            vo.setAssigneeName(assignee.getUsername());
        }
        
        return vo;
    }
}
