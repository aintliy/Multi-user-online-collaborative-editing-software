package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.AddCollaboratorRequest;
import com.example.demo.dto.CollaboratorVO;
import com.example.demo.entity.Document;
import com.example.demo.entity.DocumentCollaborator;
import com.example.demo.entity.User;
import com.example.demo.mapper.DocumentCollaboratorMapper;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档协作者服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCollaboratorService {
    
    private final DocumentCollaboratorMapper collaboratorMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    
    /**
     * 添加协作者
     */
    @Transactional(rollbackFor = Exception.class)
    public void addCollaborator(Long documentId, Long userId, AddCollaboratorRequest request) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有所有者可以添加协作者
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        // 检查目标用户是否存在
        User targetUser = userMapper.selectById(request.getUserId());
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 不能添加自己
        if (request.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF);
        }
        
        // 检查是否已经是协作者
        Long count = collaboratorMapper.selectCount(
            new LambdaQueryWrapper<DocumentCollaborator>()
                .eq(DocumentCollaborator::getDocumentId, documentId)
                .eq(DocumentCollaborator::getUserId, request.getUserId())
                .isNull(DocumentCollaborator::getRemovedAt)
        );
        
        if (count > 0) {
            throw new BusinessException(ErrorCode.COLLABORATOR_ALREADY_EXISTS);
        }
        
        // 添加协作者
        DocumentCollaborator collaborator = new DocumentCollaborator();
        collaborator.setDocumentId(documentId);
        collaborator.setUserId(request.getUserId());
        collaborator.setRole(request.getRole() != null ? request.getRole() : "VIEWER");
        collaborator.setInvitedBy(userId);
        collaborator.setCreatedAt(LocalDateTime.now());
        
        collaboratorMapper.insert(collaborator);
        
        // 发送通知
        notificationService.createNotification(
            request.getUserId(),
            "COLLABORATOR_ADDED",
            userId + " 邀请你协作文档: " + document.getTitle(),
            documentId
        );
        
        // 记录操作日志
        operationLogService.log(userId, "ADD_COLLABORATOR", "DOC", documentId, 
            "添加协作者: " + targetUser.getUsername() + " (" + collaborator.getRole() + ")");
        
        log.info("添加协作者: docId={}, userId={}, collaboratorId={}, role={}", 
            documentId, userId, request.getUserId(), collaborator.getRole());
    }
    
    /**
     * 移除协作者
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeCollaborator(Long documentId, Long userId, Long collaboratorUserId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有所有者可以移除协作者
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        DocumentCollaborator collaborator = collaboratorMapper.selectOne(
            new LambdaQueryWrapper<DocumentCollaborator>()
                .eq(DocumentCollaborator::getDocumentId, documentId)
                .eq(DocumentCollaborator::getUserId, collaboratorUserId)
                .isNull(DocumentCollaborator::getRemovedAt)
        );
        
        if (collaborator == null) {
            throw new BusinessException(ErrorCode.COLLABORATOR_NOT_FOUND);
        }
        
        // 软删除
        collaborator.setRemovedAt(LocalDateTime.now());
        collaboratorMapper.updateById(collaborator);
        
        // 发送通知
        notificationService.createNotification(
            collaboratorUserId,
            "COLLABORATOR_REMOVED",
            "你已被移出文档: " + document.getTitle(),
            documentId
        );
        
        // 记录操作日志
        operationLogService.log(userId, "REMOVE_COLLABORATOR", "DOC", documentId, 
            "移除协作者: " + collaboratorUserId);
        
        log.info("移除协作者: docId={}, userId={}, collaboratorId={}", 
            documentId, userId, collaboratorUserId);
    }
    
    /**
     * 更新协作者权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCollaboratorRole(Long documentId, Long userId, Long collaboratorUserId, String newRole) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有所有者可以更新权限
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        DocumentCollaborator collaborator = collaboratorMapper.selectOne(
            new LambdaQueryWrapper<DocumentCollaborator>()
                .eq(DocumentCollaborator::getDocumentId, documentId)
                .eq(DocumentCollaborator::getUserId, collaboratorUserId)
                .isNull(DocumentCollaborator::getRemovedAt)
        );
        
        if (collaborator == null) {
            throw new BusinessException(ErrorCode.COLLABORATOR_NOT_FOUND);
        }
        
        String oldRole = collaborator.getRole();
        collaborator.setRole(newRole);
        collaboratorMapper.updateById(collaborator);
        
        // 发送通知
        notificationService.createNotification(
            collaboratorUserId,
            "COLLABORATOR_ROLE_CHANGED",
            "文档 " + document.getTitle() + " 的协作权限已更改为: " + newRole,
            documentId
        );
        
        // 记录操作日志
        operationLogService.log(userId, "UPDATE_COLLABORATOR_ROLE", "DOC", documentId, 
            "更新协作者权限: " + collaboratorUserId + " (" + oldRole + " -> " + newRole + ")");
        
        log.info("更新协作者权限: docId={}, userId={}, collaboratorId={}, oldRole={}, newRole={}", 
            documentId, userId, collaboratorUserId, oldRole, newRole);
    }
    
    /**
     * 获取协作者列表
     */
    public List<CollaboratorVO> getCollaborators(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查权限（所有者或协作者可查看）
        if (!document.getOwnerId().equals(userId)) {
            Long count = collaboratorMapper.selectCount(
                new LambdaQueryWrapper<DocumentCollaborator>()
                    .eq(DocumentCollaborator::getDocumentId, documentId)
                    .eq(DocumentCollaborator::getUserId, userId)
                    .isNull(DocumentCollaborator::getRemovedAt)
            );
            if (count == 0) {
                throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
            }
        }
        
        List<DocumentCollaborator> collaborators = collaboratorMapper.selectList(
            new LambdaQueryWrapper<DocumentCollaborator>()
                .eq(DocumentCollaborator::getDocumentId, documentId)
                .isNull(DocumentCollaborator::getRemovedAt)
        );
        
        return collaborators.stream()
            .map(this::convertToCollaboratorVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换为CollaboratorVO
     */
    private CollaboratorVO convertToCollaboratorVO(DocumentCollaborator collaborator) {
        CollaboratorVO vo = new CollaboratorVO();
        vo.setId(collaborator.getId());
        vo.setDocumentId(collaborator.getDocumentId());
        vo.setUserId(collaborator.getUserId());
        vo.setRole(collaborator.getRole());
        vo.setCreatedAt(collaborator.getCreatedAt());
        
        User user = userMapper.selectById(collaborator.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
        }
        
        return vo;
    }
}
