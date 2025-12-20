package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateDocumentRequest;
import com.example.demo.dto.DocumentVO;
import com.example.demo.dto.ShareDocumentRequest;
import com.example.demo.dto.UpdateDocumentRequest;
import com.example.demo.entity.DocumentVersion;
import com.example.demo.service.DocumentService;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * 创建文档
     * POST /api/documents
     */
    @PostMapping
    public Result<DocumentVO> createDocument(@Validated @RequestBody CreateDocumentRequest request,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        DocumentVO document = documentService.createDocument(userId, request);
        return Result.success("文档创建成功", document);
    }

    /**
     * 获取文档列表（分页）
     * GET /api/documents?page=1&size=10
     */
    @GetMapping
    public Result<IPage<DocumentVO>> getDocumentList(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        IPage<DocumentVO> documentPage = documentService.getDocumentList(userId, page, size);
        return Result.success(documentPage);
    }

    /**
     * 获取文档详情
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    public Result<DocumentVO> getDocumentById(@PathVariable Long id,
                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        DocumentVO document = documentService.getDocumentById(userId, id);
        return Result.success(document);
    }

    /**
     * 更新文档
     * PUT /api/documents/{id}
     */
    @PutMapping("/{id}")
    public Result<DocumentVO> updateDocument(@PathVariable Long id,
                                             @Validated @RequestBody UpdateDocumentRequest request,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        DocumentVO document = documentService.updateDocument(userId, id, request);
        return Result.success("文档更新成功", document);
    }

    /**
     * 删除文档
     * DELETE /api/documents/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        documentService.deleteDocument(userId, id);
        return Result.success("文档删除成功", null);
    }

    /**
     * 分享文档
     * POST /api/documents/share
     */
    @PostMapping("/share")
    public Result<Void> shareDocument(@Validated @RequestBody ShareDocumentRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        documentService.shareDocument(userId, request);
        return Result.success("文档分享成功", null);
    }

    /**
     * 获取文档版本列表
     * GET /api/documents/{id}/versions
     */
    @GetMapping("/{id}/versions")
    public Result<List<DocumentVersion>> getVersions(@PathVariable Long id,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<DocumentVersion> versions = documentService.getVersions(userId, id);
        return Result.success(versions);
    }
}
