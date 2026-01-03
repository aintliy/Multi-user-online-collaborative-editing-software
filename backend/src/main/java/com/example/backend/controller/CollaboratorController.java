package com.example.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.collaborator.AddCollaboratorRequest;
import com.example.backend.dto.collaborator.CollaboratorDTO;
import com.example.backend.dto.collaborator.WorkspaceRequestDTO;
import com.example.backend.entity.User;
import com.example.backend.service.CollaboratorService;
import com.example.backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 协作控制器
 */
@RestController
@RequiredArgsConstructor
public class CollaboratorController {
    
    private final CollaboratorService collaboratorService;
    private final UserService userService;
    
    /**
     * 获取文档协作者列表
     */
    @GetMapping("/api/documents/{documentId}/collaborators")
    public ApiResponse<List<CollaboratorDTO>> getCollaborators(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long documentId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<CollaboratorDTO> collaborators = collaboratorService.getCollaborators(documentId, user.getId());
        return ApiResponse.success(collaborators);
    }
    
    /**
     * 邀请协作者（发送邀请）
     */
    @PostMapping("/api/documents/{documentId}/collaborators")
    public ApiResponse<Void> inviteCollaborator(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable Long documentId,
                                                 @Valid @RequestBody AddCollaboratorRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.inviteCollaborator(documentId, user.getId(), request);
        return ApiResponse.success("邀请已发送");
    }
    
    /**
     * 移除协作者
     */
    @DeleteMapping("/api/documents/{documentId}/collaborators/{userId}")
    public ApiResponse<Void> removeCollaborator(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable Long documentId,
                                                 @PathVariable Long userId) {
        User currentUser = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.removeCollaborator(documentId, userId, currentUser.getId());
        return ApiResponse.success("移除成功");
    }
    
    /**
     * 获取当前用户拥有的所有文档的待处理协作申请
     */
    @GetMapping("/api/workspace-requests/pending")
    public ApiResponse<List<WorkspaceRequestDTO>> getMyPendingWorkspaceRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<WorkspaceRequestDTO> requests = collaboratorService.getMyWorkspaceRequests(user.getId());
        return ApiResponse.success(requests);
    }
    
    /**
     * 获取当前用户收到的待处理邀请
     */
    @GetMapping("/api/collaborator-invites/pending")
    public ApiResponse<List<WorkspaceRequestDTO>> getMyPendingInvites(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<WorkspaceRequestDTO> invites = collaboratorService.getMyPendingInvites(user.getId());
        return ApiResponse.success(invites);
    }
    
    /**
     * 接受协作邀请
     */
    @PostMapping("/api/collaborator-invites/{inviteId}/accept")
    public ApiResponse<Void> acceptInvite(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long inviteId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.handleInvite(inviteId, user.getId(), true);
        return ApiResponse.success("已接受邀请");
    }
    
    /**
     * 拒绝协作邀请
     */
    @PostMapping("/api/collaborator-invites/{inviteId}/reject")
    public ApiResponse<Void> rejectInvite(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long inviteId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.handleInvite(inviteId, user.getId(), false);
        return ApiResponse.success("已拒绝邀请");
    }
    
    /**
     * 提交协作申请
     */
    @PostMapping("/api/documents/{documentId}/workspace-requests")
    public ApiResponse<Void> submitWorkspaceRequest(@AuthenticationPrincipal UserDetails userDetails,
                                                     @PathVariable Long documentId,
                                                     @RequestBody(required = false) WorkspaceRequestDTO request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.submitWorkspaceRequest(documentId, user.getId(), request);
        return ApiResponse.success("申请已提交");
    }
    
    /**
     * 审批协作申请
     */
    @PostMapping("/api/documents/{documentId}/workspace-requests/{requestId}/approve")
    public ApiResponse<Void> approveWorkspaceRequest(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long documentId,
                                                      @PathVariable Long requestId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.approveWorkspaceRequest(documentId, requestId, user.getId());
        return ApiResponse.success("审批通过");
    }
    
    /**
     * 拒绝协作申请
     */
    @PostMapping("/api/documents/{documentId}/workspace-requests/{requestId}/reject")
    public ApiResponse<Void> rejectWorkspaceRequest(@AuthenticationPrincipal UserDetails userDetails,
                                                     @PathVariable Long documentId,
                                                     @PathVariable Long requestId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.rejectWorkspaceRequest(documentId, requestId, user.getId());
        return ApiResponse.success("已拒绝");
    }
}
