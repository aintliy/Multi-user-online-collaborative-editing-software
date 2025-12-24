package com.example.backend.service;

import com.example.backend.dto.task.CreateTaskRequest;
import com.example.backend.dto.task.TaskDTO;
import com.example.backend.dto.task.UpdateTaskRequest;
import com.example.backend.entity.Document;
import com.example.backend.entity.Task;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务服务
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final UserService userService;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    
    /**
     * 创建任务
     */
    @Transactional
    public TaskDTO createTask(Long documentId, Long userId, CreateTaskRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        // 检查编辑权限
        if (!documentService.checkDocumentEditPermission(document, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权在此文档中创建任务");
        }
        
        User creator = userService.getUserById(userId);
        
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userService.getUserById(request.getAssigneeId());
        }
        
        Task task = Task.builder()
                .document(document)
                .creator(creator)
                .assignee(assignee)
                .title(request.getTitle())
                .description(request.getDescription())
                .status("todo")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .dueDate(request.getDueDate())
                .build();
        
        task = taskRepository.save(task);
        
        // 通知被分配人
        if (assignee != null && !assignee.getId().equals(userId)) {
            notificationService.createNotification(
                    assignee.getId(),
                    "TASK_ASSIGNED",
                    task.getId(),
                    creator.getUsername() + " 在文档「" + document.getTitle() + "」中给您分配了任务：" + request.getTitle()
            );
        }
        
        return TaskDTO.fromEntity(task);
    }
    
    /**
     * 获取文档任务列表
     */
    public List<TaskDTO> getDocumentTasks(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        if (!documentService.checkDocumentAccess(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        return taskRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新任务
     */
    @Transactional
    public TaskDTO updateTask(Long taskId, Long userId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在"));
        
        // 检查权限：创建者、被分配人或文档所有者可以更新
        boolean canUpdate = task.getCreator().getId().equals(userId) ||
                (task.getAssignee() != null && task.getAssignee().getId().equals(userId)) ||
                task.getDocument().getOwner().getId().equals(userId);
        
        if (!canUpdate) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此任务");
        }
        
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            String oldStatus = task.getStatus();
            task.setStatus(request.getStatus());
            
            // 任务完成时通知创建者
            if ("done".equals(request.getStatus()) && !"done".equals(oldStatus)) {
                if (!task.getCreator().getId().equals(userId)) {
                    User updater = userService.getUserById(userId);
                    notificationService.createNotification(
                            task.getCreator().getId(),
                            "TASK_COMPLETED",
                            task.getId(),
                            updater.getUsername() + " 完成了任务：" + task.getTitle()
                    );
                }
            }
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            User newAssignee = userService.getUserById(request.getAssigneeId());
            Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
            task.setAssignee(newAssignee);
            
            // 通知新的被分配人
            if (!request.getAssigneeId().equals(oldAssigneeId) && !request.getAssigneeId().equals(userId)) {
                User updater = userService.getUserById(userId);
                notificationService.createNotification(
                        request.getAssigneeId(),
                        "TASK_ASSIGNED",
                        task.getId(),
                        updater.getUsername() + " 将任务「" + task.getTitle() + "」分配给了您"
                );
            }
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        
        task = taskRepository.save(task);
        return TaskDTO.fromEntity(task);
    }
    
    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在"));
        
        // 只有创建者或文档所有者可以删除
        if (!task.getCreator().getId().equals(userId) && 
            !task.getDocument().getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此任务");
        }
        
        taskRepository.delete(task);
    }
}
