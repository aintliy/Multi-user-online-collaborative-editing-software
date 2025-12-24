package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.PageResponse;
import com.example.backend.dto.document.*;
import com.example.backend.entity.User;
import com.example.backend.service.DocumentService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    private final UserService userService;
    
    /**
     * 创建文档
     */
    @PostMapping
    public ApiResponse<DocumentDTO> createDocument(@AuthenticationPrincipal UserDetails userDetails,
                                                    @Valid @RequestBody CreateDocumentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentDTO document = documentService.createDocument(user.getId(), request);
        return ApiResponse.success("创建成功", document);
    }
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DocumentDTO> getDocument(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentDTO document = documentService.getDocument(id, user.getId());
        return ApiResponse.success(document);
    }
    
    /**
     * 更新文档元信息
     */
    @PutMapping("/{id}")
    public ApiResponse<DocumentDTO> updateDocument(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long id,
                                                    @RequestBody UpdateDocumentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentDTO document = documentService.updateDocument(id, user.getId(), request);
        return ApiResponse.success("更新成功", document);
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        documentService.deleteDocument(id, user.getId(), isAdmin);
        return ApiResponse.success("删除成功");
    }
    
    /**
     * 获取文档列表
     */
    @GetMapping
    public ApiResponse<PageResponse<DocumentDTO>> getDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        PageResponse<DocumentDTO> response = documentService.getDocuments(user.getId(), folderId, keyword, page, pageSize);
        return ApiResponse.success(response);
    }
    
    /**
     * 搜索公开文档
     */
    @GetMapping("/public")
    public ApiResponse<PageResponse<DocumentDTO>> searchPublicDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        PageResponse<DocumentDTO> response = documentService.searchPublicDocuments(user.getId(), keyword, page, pageSize);
        return ApiResponse.success(response);
    }
    
    /**
     * 提交文档新版本
     */
    @PostMapping("/{id}/commits")
    public ApiResponse<DocumentVersionDTO> commitDocument(@AuthenticationPrincipal UserDetails userDetails,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody CommitDocumentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentVersionDTO version = documentService.commitDocument(id, user.getId(), request);
        return ApiResponse.success("提交成功", version);
    }
    
    /**
     * 获取文档版本列表
     */
    @GetMapping("/{id}/versions")
    public ApiResponse<PageResponse<DocumentVersionDTO>> getDocumentVersions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        PageResponse<DocumentVersionDTO> response = documentService.getDocumentVersions(id, user.getId(), page, pageSize);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取文档版本详情
     */
    @GetMapping("/{id}/versions/{versionId}")
    public ApiResponse<DocumentVersionDTO> getDocumentVersion(@AuthenticationPrincipal UserDetails userDetails,
                                                               @PathVariable Long id,
                                                               @PathVariable Long versionId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentVersionDTO version = documentService.getDocumentVersion(id, versionId, user.getId());
        return ApiResponse.success(version);
    }
    
    /**
     * 回滚文档版本
     */
    @PostMapping("/{id}/versions/{versionId}/rollback")
    public ApiResponse<DocumentVersionDTO> rollbackVersion(@AuthenticationPrincipal UserDetails userDetails,
                                                            @PathVariable Long id,
                                                            @PathVariable Long versionId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentVersionDTO version = documentService.rollbackVersion(id, versionId, user.getId());
        return ApiResponse.success("回滚成功", version);
    }
    
    /**
     * 克隆文档
     */
    @PostMapping("/{id}/clone")
    public ApiResponse<DocumentDTO> cloneDocument(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long id,
                                                   @RequestBody(required = false) CloneDocumentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentDTO document = documentService.cloneDocument(id, user.getId(), request);
        return ApiResponse.success("克隆成功", document);
    }
    
    /**
     * 移动文档
     */
    @PutMapping("/{id}/move")
    public ApiResponse<DocumentDTO> moveDocument(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable Long id,
                                                  @RequestBody MoveDocumentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentDTO document = documentService.moveDocument(id, user.getId(), request);
        return ApiResponse.success("移动成功", document);
    }
}
