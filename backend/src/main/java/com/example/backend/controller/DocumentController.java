package com.example.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.PageResponse;
import com.example.backend.dto.document.CommitFromCacheRequest;
import com.example.backend.dto.document.CloneDocumentRequest;
import com.example.backend.dto.document.CommitDocumentRequest;
import com.example.backend.dto.document.CreateDocumentRequest;
import com.example.backend.dto.document.DocumentCacheResponse;
import com.example.backend.dto.document.DocumentDTO;
import com.example.backend.dto.document.DocumentVersionDTO;
import com.example.backend.dto.document.MoveDocumentRequest;
import com.example.backend.dto.document.SaveDocumentRequest;
import com.example.backend.dto.document.ShareLinkDTO;
import com.example.backend.dto.document.UpdateDocumentRequest;
import com.example.backend.entity.User;
import com.example.backend.service.DocumentService;
import com.example.backend.service.DocumentShareService;
import com.example.backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    private final DocumentShareService documentShareService;
    private final UserService userService;
    // 仅做调度，具体实现放在 Service 层
    
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
     * 导入文档
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentDTO> importDocument(@AuthenticationPrincipal UserDetails userDetails,
                                                    @RequestParam(value = "folderId", required = false) Long folderId,
                                                    @RequestPart("file") MultipartFile file) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentDTO document = documentService.importDocument(user.getId(), folderId, file);
        return ApiResponse.success("导入成功", document);
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
     * 获取协作缓存态（确认态内容、当前用户草稿、在线列表）
     */
    @GetMapping("/{id}/cache")
    public ApiResponse<DocumentCacheResponse> getDocumentCache(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentCacheResponse cache = documentService.getDocumentCache(id, user.getId());
        return ApiResponse.success(cache);
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
     * 保存至 Redis 确认态（不持久化数据库）。
     */
    @PostMapping("/{id}/cache/save")
    public ApiResponse<Void> saveDocumentCache(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable Long id,
                                               @Valid @RequestBody SaveDocumentRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        documentService.saveDocumentCache(id, user.getId(), user.getUsername(), request.getContent());
        return ApiResponse.success("保存成功");
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
     * 从 Redis 确认态提交版本
     */
    @PostMapping("/{id}/commits/from-cache")
    public ApiResponse<DocumentVersionDTO> commitDocumentFromCache(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @PathVariable Long id,
                                                                    @Valid @RequestBody CommitFromCacheRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        DocumentVersionDTO version = documentService.commitDocumentFromCache(id, user.getId(), request.getCommitMessage());
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
    
    // ========== 文档分享链接相关接口 ==========
    
    /**
     * 创建分享链接（一次性使用，仅限好友聊天分享）
     */
    @PostMapping("/{id}/share-links")
    public ApiResponse<ShareLinkDTO> createShareLink(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        ShareLinkDTO shareLink = documentShareService.createShareLink(id, user.getId());
        return ApiResponse.success("分享链接已创建", shareLink);
    }
    
    /**
     * 使用分享链接加入协作
     */
    @PostMapping("/share-links/{token}/use")
    public ApiResponse<Map<String, Long>> useShareLink(@AuthenticationPrincipal UserDetails userDetails,
                                                        @PathVariable String token) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        Long documentId = documentShareService.useShareLink(token, user.getId());
        return ApiResponse.success("已加入协作", Map.of("documentId", documentId));
    }
    
    /**
     * 获取分享链接信息
     */
    @GetMapping("/share-links/{token}")
    public ApiResponse<ShareLinkDTO> getShareLinkInfo(@PathVariable String token) {
        ShareLinkDTO shareLink = documentShareService.getShareLinkInfo(token);
        return ApiResponse.success(shareLink);
    }
}
