package com.example.demo.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.DocumentConstants;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CreateDocumentRequest;
import com.example.demo.dto.DocumentVO;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.UpdateDocumentRequest;
import com.example.demo.entity.Document;
import com.example.demo.entity.DocumentCollaborator;
import com.example.demo.entity.User;
import com.example.demo.mapper.DocumentCollaboratorMapper;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentMapper documentMapper;
    private final DocumentCollaboratorMapper collaboratorMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    
    /**
     * 创建文档
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO createDocument(Long userId, CreateDocumentRequest request) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setOwnerId(userId);
        document.setDocType(normalizeDocType(request.getType()));
        document.setVisibility(request.getVisibility() != null ? request.getVisibility() : "private");
        document.setContent("");
        document.setStatus("active");
        document.setCreatedAt(OffsetDateTime.now());
        document.setUpdatedAt(OffsetDateTime.now());
        
        documentMapper.insert(document);
        
        // 记录操作日志
        operationLogService.log(userId, "CREATE_DOC", "DOC", document.getId(), 
            "创建文档: " + document.getTitle());
        
        log.info("创建文档: userId={}, docId={}, title={}", userId, document.getId(), document.getTitle());
        
        return convertToDocumentVO(document, userId);
    }
    
    /**
     * 获取文档详情
     */
    public DocumentVO getDocumentById(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查访问权限
        if (!hasReadPermission(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        return convertToDocumentVO(document, userId);
    }
    
    /**
     * 更新文档元信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(Long documentId, Long userId, UpdateDocumentRequest request) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有所有者可以更新元信息
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getVisibility() != null) {
            document.setVisibility(request.getVisibility());
        }
        if (request.getTags() != null) {
            document.setTags(request.getTags());
        }
        
        document.setUpdatedAt(OffsetDateTime.now());
        documentMapper.updateById(document);
        
        // 记录操作日志
        operationLogService.log(userId, "UPDATE_DOC", "DOC", documentId, 
            "更新文档元信息: " + document.getTitle());
        
        log.info("更新文档: docId={}, userId={}", documentId, userId);
    }
    
    /**
     * 删除文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有所有者或管理员可以删除
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!document.getOwnerId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        // 软删除
        document.setStatus("deleted");
        document.setUpdatedAt(OffsetDateTime.now());
        documentMapper.updateById(document);
        
        // 记录操作日志
        operationLogService.log(userId, "DELETE_DOC", "DOC", documentId, 
            "删除文档: " + document.getTitle());
        
        log.info("删除文档: docId={}, userId={}", documentId, userId);
    }
    
    /**
     * 获取文档列表
     */
    public PageResponse<DocumentVO> getDocuments(Long userId, Integer page, Integer pageSize, 
                                                  String keyword, String tag, Long ownerId) {
        Page<Document> pageObj = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getStatus, "active");
        
        // 关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Document::getTitle, keyword);
        }
        
        // 标签筛选
        if (tag != null && !tag.trim().isEmpty()) {
            wrapper.like(Document::getTags, tag);
        }
        
        // 所有者筛选
        if (ownerId != null) {
            wrapper.eq(Document::getOwnerId, ownerId);
        } else {
            // 默认查询当前用户可访问的文档
            // 1. 自己创建的文档
            // 2. 公开的文档
            // 3. 被邀请协作的文档
            wrapper.and(w -> w
                .eq(Document::getOwnerId, userId)
                .or()
                .eq(Document::getVisibility, "public")
                .or()
                .in(Document::getId, getCollaboratedDocumentIds(userId))
            );
        }
        
        wrapper.orderByDesc(Document::getUpdatedAt);
        
        IPage<Document> result = documentMapper.selectPage(pageObj, wrapper);
        
        List<DocumentVO> items = result.getRecords().stream()
            .map(doc -> convertToDocumentVO(doc, userId))
            .collect(Collectors.toList());
        
        return new PageResponse<>(items, page, pageSize, result.getTotal());
    }
    
    /**
     * 克隆文档到个人仓库
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO cloneDocument(Long documentId, Long userId) {
        Document sourceDoc = documentMapper.selectById(documentId);
        if (sourceDoc == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查是否有查看权限
        if (!hasReadPermission(sourceDoc, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        // 创建克隆文档
        Document clonedDoc = new Document();
        clonedDoc.setTitle(sourceDoc.getTitle() + " (副本)");
        clonedDoc.setOwnerId(userId);
        clonedDoc.setContent(sourceDoc.getContent());
        clonedDoc.setDocType(sourceDoc.getDocType());
        clonedDoc.setVisibility("private"); // 克隆后默认私有
        clonedDoc.setForkedFromId(documentId);
        clonedDoc.setStatus("active");
        clonedDoc.setCreatedAt(OffsetDateTime.now());
        clonedDoc.setUpdatedAt(OffsetDateTime.now());
        
        documentMapper.insert(clonedDoc);
        
        // 记录操作日志
        operationLogService.log(userId, "CLONE_DOC", "DOC", clonedDoc.getId(), 
            "克隆文档: " + sourceDoc.getTitle());
        
        log.info("克隆文档: sourceDocId={}, newDocId={}, userId={}", 
            documentId, clonedDoc.getId(), userId);
        
        return convertToDocumentVO(clonedDoc, userId);
    }
    
    /**
     * 检查用户是否有读权限
     */
    private boolean hasReadPermission(Document document, Long userId) {
        // 1. 所有者
        if (document.getOwnerId().equals(userId)) {
            return true;
        }
        
        // 2. 公开文档
        if ("public".equals(document.getVisibility())) {
            return true;
        }
        
        // 3. 协作者
        Long count = collaboratorMapper.selectCount(
            new LambdaQueryWrapper<DocumentCollaborator>()
                .eq(DocumentCollaborator::getDocumentId, document.getId())
                .eq(DocumentCollaborator::getUserId, userId)
                .isNull(DocumentCollaborator::getRemovedAt)
        );
        
        return count > 0;
    }
    
    /**
     * 检查用户是否有编辑权限
     */
    public boolean hasEditPermission(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            return false;
        }
        
        // 1. 所有者
        if (document.getOwnerId().equals(userId)) {
            return true;
        }
        
        // 2. EDITOR协作者
        DocumentCollaborator collaborator = collaboratorMapper.selectOne(
            new LambdaQueryWrapper<DocumentCollaborator>()
                .eq(DocumentCollaborator::getDocumentId, documentId)
                .eq(DocumentCollaborator::getUserId, userId)
                .eq(DocumentCollaborator::getRole, "EDITOR")
                .isNull(DocumentCollaborator::getRemovedAt)
        );
        
        return collaborator != null;
    }
    
    /**
     * 获取用户协作的文档ID列表
     */
    private List<Long> getCollaboratedDocumentIds(Long userId) {
        List<DocumentCollaborator> collaborators = collaboratorMapper.selectList(
            new LambdaQueryWrapper<DocumentCollaborator>()
                .eq(DocumentCollaborator::getUserId, userId)
                .isNull(DocumentCollaborator::getRemovedAt)
        );
        
        return collaborators.stream()
            .map(DocumentCollaborator::getDocumentId)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换为DocumentVO
     */
    private DocumentVO convertToDocumentVO(Document document, Long currentUserId) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setOwnerId(document.getOwnerId());
        vo.setContent(document.getContent());
        vo.setDocType(document.getDocType());
        vo.setVisibility(document.getVisibility());
        vo.setTags(document.getTags());
        vo.setStatus(document.getStatus());
        vo.setForkedFromId(document.getForkedFromId());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());
        
        // 设置所有者信息
        User owner = userMapper.selectById(document.getOwnerId());
        if (owner != null) {
            vo.setOwnerName(owner.getUsername());
        }
        
        // 设置权限标识
        vo.setIsOwner(document.getOwnerId().equals(currentUserId));
        vo.setCanEdit(hasEditPermission(document.getId(), currentUserId));
        
        return vo;
    }

    private String normalizeDocType(String docType) {
        if (docType == null || docType.trim().isEmpty()) {
            return DocumentConstants.DEFAULT_DOC_TYPE;
        }
        String normalized = docType.trim().toLowerCase();
        if (!DocumentConstants.SUPPORTED_DOC_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_INVALID);
        }
        return normalized;
    }
}
