package com.example.backend.service;

import com.example.backend.dto.collaborator.*;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.*;
import com.example.backend.util.IdGenerator;

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
    private final DocumentInviteLinkRepository inviteLinkRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    
    /**
     * 获取文档协作者列表（包含所有者）
     */
    public List<CollaboratorDTO> getCollaborators(Long documentId, Long userId) {
        Document document = getDocumentById(documentId);
        
        // // 只有所有者或协作者可以查看协作者列表
        // boolean isOwner = document.getOwner().getId().equals(userId);
        // boolean isCollaborator = collaboratorRepository.existsByDocumentIdAndUserId(documentId, userId);
        
        // if (!isOwner && !isCollaborator) {
        //     throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者或协作者可以查看协作者列表");
        // }
        
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
     * 添加协作者
     */
    @Transactional
    public CollaboratorDTO addCollaborator(Long documentId, Long userId, AddCollaboratorRequest request) {
        Document document = getDocumentById(documentId);
        
        // 只有所有者可以添加协作者
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以添加协作者");
        }
        
        // 检查是否已经是协作者
        if (collaboratorRepository.existsByDocumentIdAndUserId(documentId, request.getUserId())) {
            throw new BusinessException(ErrorCode.COLLABORATOR_ALREADY_EXISTS, "该用户已是协作者");
        }
        
        // 不能添加自己
        if (request.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能将自己添加为协作者");
        }
        
        User targetUser = userService.getUserById(request.getUserId());
        User inviter = userService.getUserById(userId);
        
        DocumentCollaborator collaborator = DocumentCollaborator.builder()
                .document(document)
                .user(targetUser)
                .invitedBy(inviter)
                .build();
        
        collaborator = collaboratorRepository.save(collaborator);
        
        // 发送通知
        notificationService.createNotification(
                request.getUserId(),
                "COLLABORATOR_ADDED",
                documentId,
                inviter.getUsername() + " 邀请您协作编辑文档「" + document.getTitle() + "」"
        );
        
        return CollaboratorDTO.fromEntity(collaborator);
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
    
    // /**
    //  * 获取协作申请列表
    //  */
    // public List<DocumentWorkspaceRequest> getWorkspaceRequests(Long documentId, Long userId) {
    //     Document document = getDocumentById(documentId);
        
    //     // 只有所有者可以查看申请列表
    //     if (!document.getOwner().getId().equals(userId)) {
    //         throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以查看协作申请");
    //     }
        
    //     return workspaceRequestRepository.findByDocumentIdAndStatus(documentId, "PENDING");
    // }
    
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
    
    /**
     * 生成邀请链接
     */
    @Transactional
    public String createInviteLink(Long documentId, Long userId, CreateInviteLinkRequest request) {
        Document document = getDocumentById(documentId);
        
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以生成邀请链接");
        }
        
        User creator = userService.getUserById(userId);
        String token = IdGenerator.generateToken();
        
        DocumentInviteLink inviteLink = DocumentInviteLink.builder()
                .document(document)
                .token(token)
                .maxUses(request != null ? request.getMaxUses() : null)
                .expiresAt(request != null ? request.getExpiresAt() : null)
                .createdBy(creator)
                .build();
        
        inviteLinkRepository.save(inviteLink);
        
        return token;
    }
    
    /**
     * 通过邀请链接加入协作
     */
    @Transactional
    public void joinByInvite(Long userId, String token) {
        DocumentInviteLink inviteLink = inviteLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_LINK_INVALID, "邀请链接无效"));
        
        // 检查是否过期
        if (inviteLink.getExpiresAt() != null && inviteLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVITE_LINK_INVALID, "邀请链接已过期");
        }
        
        // 检查使用次数
        if (inviteLink.getMaxUses() != null && inviteLink.getUsedCount() >= inviteLink.getMaxUses()) {
            throw new BusinessException(ErrorCode.INVITE_LINK_INVALID, "邀请链接已达到最大使用次数");
        }
        
        Document document = inviteLink.getDocument();
        
        // 检查是否已经是协作者
        if (collaboratorRepository.existsByDocumentIdAndUserId(document.getId(), userId)) {
            throw new BusinessException(ErrorCode.COLLABORATOR_ALREADY_EXISTS, "您已是该文档的协作者");
        }
        
        // 不能加入自己的文档
        if (document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能加入自己的文档");
        }
        
        User user = userService.getUserById(userId);
        
        // 添加为协作者
        DocumentCollaborator collaborator = DocumentCollaborator.builder()
                .document(document)
                .user(user)
                .invitedBy(inviteLink.getCreatedBy())
                .build();
        collaboratorRepository.save(collaborator);
        
        // 更新使用次数
        inviteLink.setUsedCount(inviteLink.getUsedCount() + 1);
        inviteLinkRepository.save(inviteLink);
    }
    
    private Document getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
    }
}
