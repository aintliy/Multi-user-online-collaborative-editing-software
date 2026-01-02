package com.example.backend.controller;

import java.util.List;
import java.util.Map;

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
import com.example.backend.dto.collaborator.CreateInviteLinkRequest;
import com.example.backend.dto.collaborator.JoinByInviteRequest;
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
     * 添加协作者
     */
    @PostMapping("/api/documents/{documentId}/collaborators")
    public ApiResponse<CollaboratorDTO> addCollaborator(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable Long documentId,
                                                         @Valid @RequestBody AddCollaboratorRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        CollaboratorDTO collaborator = collaboratorService.addCollaborator(documentId, user.getId(), request);
        return ApiResponse.success("添加成功", collaborator);
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
    
    // /**
    //  * 获取协作申请列表
    //  */
    // @GetMapping("/api/documents/{documentId}/workspace-requests")
    // public ApiResponse<List<DocumentWorkspaceRequest>> getWorkspaceRequests(@AuthenticationPrincipal UserDetails userDetails,
    //                                                                          @PathVariable Long documentId) {
    //     User user = userService.getUserByEmail(userDetails.getUsername());
    //     List<DocumentWorkspaceRequest> requests = collaboratorService.getWorkspaceRequests(documentId, user.getId());
    //     return ApiResponse.success(requests);
    // }
    
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
    
    /**
     * 生成邀请链接
     */
    @PostMapping("/api/documents/{documentId}/invite-links")
    public ApiResponse<Map<String, String>> createInviteLink(@AuthenticationPrincipal UserDetails userDetails,
                                                              @PathVariable Long documentId,
                                                              @RequestBody(required = false) CreateInviteLinkRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        String token = collaboratorService.createInviteLink(documentId, user.getId(), request);
        String inviteUrl = "http://localhost:3000/join?token=" + token;
        return ApiResponse.success(Map.of("token", token, "inviteUrl", inviteUrl));
    }
    
    /**
     * 通过邀请链接加入协作
     */
    @PostMapping("/api/documents/join-by-invite")
    public ApiResponse<Void> joinByInvite(@AuthenticationPrincipal UserDetails userDetails,
                                           @Valid @RequestBody JoinByInviteRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        collaboratorService.joinByInvite(user.getId(), request.getToken());
        return ApiResponse.success("加入成功");
    }
}
