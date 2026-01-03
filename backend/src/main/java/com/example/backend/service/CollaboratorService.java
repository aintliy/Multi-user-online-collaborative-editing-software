package com.example.backend.service;

import com.example.backend.dto.collaborator.*;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 协作服务
 */
@Service
@RequiredArgsConstructor
public class CollaboratorService {
    
    private final DocumentRepository documentRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final DocumentWorkspaceRequestRepository workspaceRequestRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    
    /**
     * 获取文档协作者列表（包含所有者）
     */
    public List<CollaboratorDTO> getCollaborators(Long documentId, Long userId) {
        Document document = getDocumentById(documentId);      
        
        List<CollaboratorDTO> result = new ArrayList<>();
        
        // 添加所有者
        result.add(CollaboratorDTO.fromOwner(document));
        
        // 添加协作者
        collaboratorRepository.findByDocumentId(documentId).stream()
                .map(CollaboratorDTO::fromEntity)
                .forEach(result::add);
        
        return result;
    }
    
    /**
     * 邀请协作者（发送邀请，需要对方同意）
     */
    @Transactional
    public void inviteCollaborator(Long documentId, Long userId, AddCollaboratorRequest request) {
        Document document = getDocumentById(documentId);
        
        // 只有所有者可以邀请协作者
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以邀请协作者");
        }
        
        // 检查是否已经是协作者
        if (collaboratorRepository.existsByDocumentIdAndUserId(documentId, request.getUserId())) {
            throw new BusinessException(ErrorCode.COLLABORATOR_ALREADY_EXISTS, "该用户已是协作者");
        }
        
        // 不能邀请自己
        if (request.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能邀请自己");
        }
        
        // 检查是否已有待处理的邀请
        if (workspaceRequestRepository.existsByDocumentIdAndApplicantIdAndTypeAndStatus(
                documentId, request.getUserId(), "INVITE", "PENDING")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已向该用户发送过邀请，请等待对方处理");
        }
        
        User targetUser = userService.getUserById(request.getUserId());
        User inviter = userService.getUserById(userId);
        
        // 创建邀请记录
        DocumentWorkspaceRequest invite = DocumentWorkspaceRequest.builder()
                .document(document)
                .applicant(targetUser)
                .type("INVITE")
                .status("PENDING")
                .message(inviter.getUsername() + " 邀请您协作编辑此文档")
                .build();
        
        workspaceRequestRepository.save(invite);
        
        // 发送通知
        notificationService.createNotification(
                request.getUserId(),
                "COLLABORATOR_INVITE",
                documentId,
                inviter.getUsername() + " 邀请您协作编辑文档「" + document.getTitle() + "」"
        );
    }
    
    /**
     * 处理协作邀请（被邀请者同意/拒绝）
     */
    @Transactional
    public void handleInvite(Long inviteId, Long userId, boolean accept) {
        DocumentWorkspaceRequest invite = workspaceRequestRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "邀请不存在"));
        
        // 只有被邀请者可以处理邀请
        if (!invite.getApplicant().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理此邀请");
        }
        
        // 检查状态
        if (!"PENDING".equals(invite.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该邀请已处理");
        }
        
        // 检查是否是邀请类型
        if (!"INVITE".equals(invite.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该请求不是邀请类型");
        }
        
        Document document = invite.getDocument();
        User invitedUser = invite.getApplicant();
        
        if (accept) {
            // 检查是否已经是协作者
            if (collaboratorRepository.existsByDocumentIdAndUserId(document.getId(), userId)) {
                throw new BusinessException(ErrorCode.COLLABORATOR_ALREADY_EXISTS, "您已是该文档的协作者");
            }
            
            // 添加为协作者
            DocumentCollaborator collaborator = DocumentCollaborator.builder()
                    .document(document)
                    .user(invitedUser)
                    .invitedBy(document.getOwner())
                    .build();
            collaboratorRepository.save(collaborator);
            
            invite.setStatus("APPROVED");
            
            // 通知文档所有者
            notificationService.createNotification(
                    document.getOwner().getId(),
                    "COLLABORATOR_INVITE_ACCEPTED",
                    document.getId(),
                    invitedUser.getUsername() + " 已接受您的协作邀请，加入了文档「" + document.getTitle() + "」"
            );
        } else {
            invite.setStatus("REJECTED");
            
            // 通知文档所有者
            notificationService.createNotification(
                    document.getOwner().getId(),
                    "COLLABORATOR_INVITE_REJECTED",
                    document.getId(),
                    invitedUser.getUsername() + " 拒绝了您的协作邀请"
            );
        }
        
        invite.setHandledAt(LocalDateTime.now());
        invite.setHandledBy(invitedUser);
        workspaceRequestRepository.save(invite);
    }
    
    /**
     * 获取用户收到的待处理邀请
     */
    public List<WorkspaceRequestDTO> getMyPendingInvites(Long userId) {
        return workspaceRequestRepository.findPendingInvitesByUserId(userId, "PENDING").stream()
                .map(WorkspaceRequestDTO::fromEntity)
                .toList();
    }
    
    /**
     * 移除协作者
     */
    @Transactional
    public void removeCollaborator(Long documentId, Long collaboratorUserId, Long userId) {
        Document document = getDocumentById(documentId);
        
        // 只有所有者可以移除协作者
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以移除协作者");
        }
        
        DocumentCollaborator collaborator = collaboratorRepository.findByDocumentIdAndUserId(documentId, collaboratorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COLLABORATOR_NOT_FOUND, "协作者不存在"));
        
        collaboratorRepository.delete(collaborator);
        
        // 发送通知
        notificationService.createNotification(
                collaboratorUserId,
                "COLLABORATOR_REMOVED",
                documentId,
                "您已被移出文档「" + document.getTitle() + "」的协作工作区"
        );
    }
    
    /**
     * 提交协作申请
     */
    @Transactional
    public void submitWorkspaceRequest(Long documentId, Long userId, WorkspaceRequestDTO request) {
        Document document = getDocumentById(documentId);
        
        // 只能申请公开文档
        if (!"public".equals(document.getVisibility())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能申请加入公开文档的协作工作区");
        }
        
        // 不能申请自己的文档
        if (document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能申请加入自己的文档");
        }
        
        // 检查是否已经是协作者
        if (collaboratorRepository.existsByDocumentIdAndUserId(documentId, userId)) {
            throw new BusinessException(ErrorCode.COLLABORATOR_ALREADY_EXISTS, "您已是该文档的协作者");
        }
        
        // 检查是否已有待处理的申请
        if (workspaceRequestRepository.existsByDocumentIdAndApplicantIdAndStatus(documentId, userId, "PENDING")) {
            throw new BusinessException(ErrorCode.WORKSPACE_REQUEST_EXISTS, "您已提交过申请，请等待审核");
        }
        
        User applicant = userService.getUserById(userId);
        
        DocumentWorkspaceRequest workspaceRequest = DocumentWorkspaceRequest.builder()
                .document(document)
                .applicant(applicant)
                .message(request != null ? request.getMessage() : null)
                .status("PENDING")
                .build();
        
        workspaceRequestRepository.save(workspaceRequest);
        
        // 通知文档所有者
        notificationService.createNotification(
                document.getOwner().getId(),
                "WORKSPACE_REQUEST",
                documentId,
                applicant.getUsername() + " 申请加入文档「" + document.getTitle() + "」的协作工作区"
        );
    }
    
    /**
     * 获取用户拥有的所有文档的待处理协作申请
     */
    public List<WorkspaceRequestDTO> getMyWorkspaceRequests(Long userId) {
        
        List<WorkspaceRequestDTO> result = new ArrayList<>();

        workspaceRequestRepository.findByDocumentOwnerIdAndStatus(userId, "PENDING").stream()
                .map(WorkspaceRequestDTO::fromEntity)
                .forEach(result::add);

        return result;
    }
    
    /**
     * 审批协作申请
     */
    @Transactional
    public void approveWorkspaceRequest(Long documentId, Long requestId, Long userId) {
        Document document = getDocumentById(documentId);
        
        // 只有所有者可以审批
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以审批协作申请");
        }
        
        DocumentWorkspaceRequest request = workspaceRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_REQUEST_NOT_FOUND, "申请不存在"));
        
        if (!request.getDocument().getId().equals(documentId)) {
            throw new BusinessException(ErrorCode.WORKSPACE_REQUEST_NOT_FOUND, "申请不存在");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该申请已被处理");
        }
        
        User handler = userService.getUserById(userId);
        
        // 更新申请状态
        request.setStatus("APPROVED");
        request.setHandledAt(LocalDateTime.now());
        request.setHandledBy(handler);
        workspaceRequestRepository.save(request);
        
        // 添加为协作者
        DocumentCollaborator collaborator = DocumentCollaborator.builder()
                .document(document)
                .user(request.getApplicant())
                .invitedBy(handler)
                .build();
        collaboratorRepository.save(collaborator);
        
        // 通知申请人
        notificationService.createNotification(
                request.getApplicant().getId(),
                "WORKSPACE_REQUEST_APPROVED",
                documentId,
                "您加入文档「" + document.getTitle() + "」协作工作区的申请已通过"
        );
    }
    
    /**
     * 拒绝协作申请
     */
    @Transactional
    public void rejectWorkspaceRequest(Long documentId, Long requestId, Long userId) {
        Document document = getDocumentById(documentId);
        
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以审批协作申请");
        }
        
        DocumentWorkspaceRequest request = workspaceRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_REQUEST_NOT_FOUND, "申请不存在"));
        
        if (!request.getDocument().getId().equals(documentId)) {
            throw new BusinessException(ErrorCode.WORKSPACE_REQUEST_NOT_FOUND, "申请不存在");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该申请已被处理");
        }
        
        User handler = userService.getUserById(userId);
        
        request.setStatus("REJECTED");
        request.setHandledAt(LocalDateTime.now());
        request.setHandledBy(handler);
        workspaceRequestRepository.save(request);
        
        // 通知申请人
        notificationService.createNotification(
                request.getApplicant().getId(),
                "WORKSPACE_REQUEST_REJECTED",
                documentId,
                "您加入文档「" + document.getTitle() + "」协作工作区的申请被拒绝"
        );
    }
    
    private Document getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
    }
}
