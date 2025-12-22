package com.example.demo.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.AddCollaboratorRequest;
import com.example.demo.dto.CollaboratorVO;
import com.example.demo.dto.CommitDocumentRequest;
import com.example.demo.dto.CreateDocumentRequest;
import com.example.demo.dto.DocumentVO;
import com.example.demo.dto.DocumentVersionVO;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.UpdateDocumentRequest;
import com.example.demo.service.DocumentCollaboratorService;
import com.example.demo.service.DocumentService;
import com.example.demo.service.DocumentVersionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    private final DocumentVersionService versionService;
    private final DocumentCollaboratorService collaboratorService;
    
    /**
     * 创建文档
     */
    @PostMapping
    public ApiResponse<DocumentVO> createDocument(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateDocumentRequest request) {
        DocumentVO document = documentService.createDocument(userId, request);
        return ApiResponse.success(document);
    }
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DocumentVO> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        DocumentVO document = documentService.getDocumentById(id, userId);
        return ApiResponse.success(document);
    }
    
    /**
     * 更新文档元信息
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateDocumentRequest request) {
        documentService.updateDocument(id, userId, request);
        return ApiResponse.success();
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        documentService.deleteDocument(id, userId);
        return ApiResponse.success();
    }
    
    /**
     * 获取文档列表
     */
    @GetMapping
    public ApiResponse<PageResponse<DocumentVO>> getDocuments(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long ownerId) {
        PageResponse<DocumentVO> documents = documentService.getDocuments(
                userId, page, pageSize, keyword, tag, ownerId);
        return ApiResponse.success(documents);
    }
    
    /**
     * 克隆文档
     */
    @PostMapping("/{id}/clone")
    public ApiResponse<DocumentVO> cloneDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        DocumentVO document = documentService.cloneDocument(id, userId);
        return ApiResponse.success(document);
    }
    
    // ============ 版本控制 ============
    
    /**
     * 提交版本
     */
    @PostMapping("/{id}/commit")
    public ApiResponse<DocumentVersionVO> commitVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommitDocumentRequest request) {
        DocumentVersionVO version = versionService.commitVersion(id, userId, request);
        return ApiResponse.success(version);
    }
    
    /**
     * 获取版本历史
     */
    @GetMapping("/{id}/versions")
    public ApiResponse<List<DocumentVersionVO>> getVersionHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        List<DocumentVersionVO> versions = versionService.getVersionHistory(id, userId);
        return ApiResponse.success(versions);
    }
    
    /**
     * 获取特定版本
     */
    @GetMapping("/versions/{versionId}")
    public ApiResponse<DocumentVersionVO> getVersion(
            @PathVariable Long versionId,
            @AuthenticationPrincipal Long userId) {
        DocumentVersionVO version = versionService.getVersion(versionId, userId);
        return ApiResponse.success(version);
    }
    
    /**
     * 回滚到指定版本
     */
    @PostMapping("/{id}/rollback")
    public ApiResponse<DocumentVersionVO> rollbackToVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestParam Integer versionNumber) {
        DocumentVersionVO version = versionService.rollbackToVersion(id, versionNumber, userId);
        return ApiResponse.success(version);
    }
    
    // ============ 协作管理 ============
    
    /**
     * 添加协作者
     */
    @PostMapping("/{id}/collaborators")
    public ApiResponse<Void> addCollaborator(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AddCollaboratorRequest request) {
        collaboratorService.addCollaborator(id, userId, request);
        return ApiResponse.success();
    }
    
    /**
     * 获取协作者列表
     */
    @GetMapping("/{id}/collaborators")
    public ApiResponse<List<CollaboratorVO>> getCollaborators(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        List<CollaboratorVO> collaborators = collaboratorService.getCollaborators(id, userId);
        return ApiResponse.success(collaborators);
    }
    
    /**
     * 移除协作者
     */
    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    public ApiResponse<Void> removeCollaborator(
            @PathVariable Long id,
            @PathVariable Long collaboratorId,
            @AuthenticationPrincipal Long userId) {
        collaboratorService.removeCollaborator(id, userId, collaboratorId);
        return ApiResponse.success();
    }
    
    /**
     * 更新协作者权限
     */
    @PutMapping("/{id}/collaborators/{collaboratorId}")
    public ApiResponse<Void> updateCollaboratorRole(
            @PathVariable Long id,
            @PathVariable Long collaboratorId,
            @AuthenticationPrincipal Long userId,
            @RequestParam String role) {
        collaboratorService.updateCollaboratorRole(id, userId, collaboratorId, role);
        return ApiResponse.success();
    }
}
