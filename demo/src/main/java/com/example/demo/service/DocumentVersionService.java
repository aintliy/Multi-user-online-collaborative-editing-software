package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CommitDocumentRequest;
import com.example.demo.dto.DocumentVersionVO;
import com.example.demo.entity.Document;
import com.example.demo.entity.DocumentVersion;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.DocumentVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档版本服务（Git风格版本控制）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVersionService {
    
    private final DocumentVersionMapper versionMapper;
    private final DocumentMapper documentMapper;
    private final DocumentService documentService;
    private final OperationLogService operationLogService;
    
    /**
     * 提交新版本
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionVO commitVersion(Long documentId, Long userId, CommitDocumentRequest request) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查编辑权限
        if (!documentService.hasEditPermission(documentId, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        // 获取上一个版本号
        DocumentVersion lastVersion = versionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, documentId)
                .orderByDesc(DocumentVersion::getVersionNo)
                .last("LIMIT 1")
        );
        
        int newVersionNumber = lastVersion != null ? lastVersion.getVersionNo() + 1 : 1;
        
        // 创建新版本
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(documentId);
        version.setVersionNo(newVersionNumber);
        version.setContent(request.getContent());
        version.setCommitMessage(request.getCommitMessage());
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        
        versionMapper.insert(version);
        
        // 更新文档的当前内容
        document.setContent(request.getContent());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        
        // 记录操作日志
        operationLogService.log(userId, "COMMIT_VERSION", "DOC", documentId, 
            "提交版本 v" + newVersionNumber + ": " + request.getCommitMessage());
        
        log.info("提交文档版本: docId={}, version={}, userId={}", documentId, newVersionNumber, userId);
        
        return convertToVersionVO(version);
    }
    
    /**
     * 获取版本历史
     */
    public List<DocumentVersionVO> getVersionHistory(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查读权限
        if (!documentService.hasEditPermission(documentId, userId) && !document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        List<DocumentVersion> versions = versionMapper.selectList(
            new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, documentId)
                .orderByDesc(DocumentVersion::getVersionNo)
        );
        
        return versions.stream()
            .map(this::convertToVersionVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取特定版本详情
     */
    public DocumentVersionVO getVersion(Long versionId, Long userId) {
        DocumentVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        
        Document document = documentMapper.selectById(version.getDocumentId());
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查权限
        if (!documentService.hasEditPermission(document.getId(), userId) && 
            !document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        return convertToVersionVO(version);
    }
    
    /**
     * 回滚到指定版本
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionVO rollbackToVersion(Long documentId, Integer versionNumber, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有所有者可以回滚
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        // 查找目标版本
        DocumentVersion targetVersion = versionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, documentId)
                .eq(DocumentVersion::getVersionNo, versionNumber)
        );
        
        if (targetVersion == null) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        
        // 获取当前最新版本号
        DocumentVersion lastVersion = versionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, documentId)
                .orderByDesc(DocumentVersion::getVersionNo)
                .last("LIMIT 1")
        );
        
        int newVersionNumber = lastVersion != null ? lastVersion.getVersionNo() + 1 : 1;
        
        // 创建新版本
        DocumentVersion newVersion = new DocumentVersion();
        newVersion.setDocumentId(documentId);
        newVersion.setVersionNo(newVersionNumber);
        newVersion.setContent(targetVersion.getContent());
        newVersion.setCommitMessage("回滚到版本 " + versionNumber);
        newVersion.setCreatedBy(userId);
        newVersion.setCreatedAt(LocalDateTime.now());
        
        versionMapper.insert(newVersion);
        
        // 更新文档内容
        document.setContent(targetVersion.getContent());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        
        // 记录操作日志
        operationLogService.log(userId, "ROLLBACK_VERSION", "DOC", documentId, 
            "回滚到版本 v" + versionNumber);
        
        log.info("回滚文档版本: docId={}, fromVersion={}, toVersion={}, userId={}", 
            documentId, newVersionNumber, versionNumber, userId);
        
        return convertToVersionVO(newVersion);
    }
    
    /**
     * 比较两个版本
     */
    public String compareVersions(Long documentId, Integer version1, Integer version2, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        // 检查权限
        if (!documentService.hasEditPermission(documentId, userId) && 
            !document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }
        
        DocumentVersion v1 = versionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, documentId)
                .eq(DocumentVersion::getVersionNo, version1)
        );
        
        DocumentVersion v2 = documentVersionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, documentId)
                .eq(DocumentVersion::getVersionNo, version2)
        );
        
        if (v1 == null || v2 == null) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        
        // 简单的差异说明（实际可以使用diff库）
        return String.format("版本 v%d (%d 字符) vs 版本 v%d (%d 字符)", 
            version1, v1.getContent().length(), version2, v2.getContent().length());
    }
    
    /**
     * 转换为VersionVO
     */
    private DocumentVersionVO convertToVersionVO(DocumentVersion version) {
        DocumentVersionVO vo = new DocumentVersionVO();
        vo.setId(version.getId());
        vo.setDocumentId(version.getDocumentId());
        vo.setVersionNo(version.getVersionNo());
        vo.setContent(version.getContent());
        vo.setCommitMessage(version.getCommitMessage());
        vo.setCreatedBy(version.getCreatedBy());
        vo.setCreatedAt(version.getCreatedAt());
        return vo;
    }
}
