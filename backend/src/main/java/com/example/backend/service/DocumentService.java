package com.example.backend.service;

import com.example.backend.dto.PageResponse;
import com.example.backend.dto.document.*;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final DocumentFolderRepository folderRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserService userService;
    
    /**
     * 创建文档
     */
    @Transactional
    public DocumentDTO createDocument(Long userId, CreateDocumentRequest request) {
        User owner = userService.getUserById(userId);
        
        DocumentFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在"));
            // 验证文件夹是否属于当前用户
            if (!folder.getOwner().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此文件夹");
            }
        }
        
        Document document = Document.builder()
                .title(request.getTitle())
                .owner(owner)
                .docType(request.getType() != null ? request.getType() : "markdown")
                .visibility(request.getVisibility() != null ? request.getVisibility() : "private")
                .folder(folder)
                .content("")
                .status("active")
                .build();
        
        document = documentRepository.save(document);
        
        // 创建初始版本
        createInitialVersion(document, owner);
        
        return DocumentDTO.fromEntity(document, userId, true);
    }
    
    /**
     * 获取文档详情
     */
    public DocumentDTO getDocument(Long documentId, Long userId) {
        Document document = getDocumentById(documentId);
        
        // 检查访问权限
        boolean canAccess = checkDocumentAccess(document, userId);
        if (!canAccess) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        boolean canEdit = checkDocumentEditPermission(document, userId);
        return DocumentDTO.fromEntity(document, userId, canEdit);
    }
    
    /**
     * 更新文档元信息
     */
    @Transactional
    public DocumentDTO updateDocument(Long documentId, Long userId, UpdateDocumentRequest request) {
        Document document = getDocumentById(documentId);
        
        // 只有所有者可以更新元信息
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以更新文档信息");
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
        
        document = documentRepository.save(document);
        return DocumentDTO.fromEntity(document, userId, true);
    }
    
    /**
     * 提交文档新版本
     */
    @Transactional
    public DocumentVersionDTO commitDocument(Long documentId, Long userId, CommitDocumentRequest request) {
        Document document = getDocumentById(documentId);
        
        // 检查编辑权限
        if (!checkDocumentEditPermission(document, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此文档");
        }
        
        User user = userService.getUserById(userId);
        
        // 获取最新版本号
        Integer maxVersionNo = versionRepository.findMaxVersionNoByDocumentId(documentId);
        int newVersionNo = (maxVersionNo == null ? 0 : maxVersionNo) + 1;
        
        // 创建新版本
        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNo(newVersionNo)
                .content(request.getContent())
                .commitMessage(request.getCommitMessage())
                .createdBy(user)
                .build();
        
        version = versionRepository.save(version);
        
        // 更新文档内容
        document.setContent(request.getContent());
        documentRepository.save(document);
        
        return DocumentVersionDTO.fromEntity(version);
    }
    
    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long documentId, Long userId, boolean isAdmin) {
        Document document = getDocumentById(documentId);
        
        // 检查权限：所有者或管理员可删除
        if (!document.getOwner().getId().equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此文档");
        }
        
        documentRepository.delete(document);
    }
    
    /**
     * 获取文档列表
     */
    public PageResponse<DocumentDTO> getDocuments(Long userId, Long folderId, String keyword, 
                                                   int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Document> documentPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            documentPage = documentRepository.searchByOwnerAndKeyword(userId, keyword, pageable);
        } else if (folderId != null) {
            documentPage = documentRepository.findByOwnerIdAndFolderId(userId, folderId, pageable);
        } else {
            documentPage = documentRepository.findByOwnerIdAndFolderIdIsNull(userId, pageable);
        }
        
        List<DocumentDTO> items = documentPage.getContent().stream()
                .map(doc -> DocumentDTO.fromEntity(doc, userId, true))
                .collect(Collectors.toList());
        
        return PageResponse.<DocumentDTO>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .total(documentPage.getTotalElements())
                .build();
    }
    
    /**
     * 搜索公开文档
     */
    public PageResponse<DocumentDTO> searchPublicDocuments(Long userId, String keyword, 
                                                            int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Document> documentPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            documentPage = documentRepository.searchPublicDocuments(keyword, pageable);
        } else {
            documentPage = documentRepository.findByVisibility("public", pageable);
        }
        
        List<DocumentDTO> items = documentPage.getContent().stream()
                .map(doc -> DocumentDTO.fromEntity(doc, userId, checkDocumentEditPermission(doc, userId)))
                .collect(Collectors.toList());
        
        return PageResponse.<DocumentDTO>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .total(documentPage.getTotalElements())
                .build();
    }
    
    /**
     * 获取文档版本列表
     */
    public PageResponse<DocumentVersionDTO> getDocumentVersions(Long documentId, Long userId, 
                                                                  int page, int pageSize) {
        Document document = getDocumentById(documentId);
        
        // 检查访问权限
        if (!checkDocumentAccess(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<DocumentVersion> versionPage = versionRepository.findByDocumentIdOrderByVersionNoDesc(documentId, pageable);
        
        List<DocumentVersionDTO> items = versionPage.getContent().stream()
                .map(DocumentVersionDTO::fromEntity)
                .collect(Collectors.toList());
        
        return PageResponse.<DocumentVersionDTO>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .total(versionPage.getTotalElements())
                .build();
    }
    
    /**
     * 获取文档版本详情
     */
    public DocumentVersionDTO getDocumentVersion(Long documentId, Long versionId, Long userId) {
        Document document = getDocumentById(documentId);
        
        if (!checkDocumentAccess(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "版本不存在"));
        
        if (!version.getDocument().getId().equals(documentId)) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND, "版本不存在");
        }
        
        return DocumentVersionDTO.fromEntity(version);
    }
    
    /**
     * 回滚文档版本
     */
    @Transactional
    public DocumentVersionDTO rollbackVersion(Long documentId, Long versionId, Long userId) {
        Document document = getDocumentById(documentId);
        
        if (!checkDocumentEditPermission(document, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此文档");
        }
        
        DocumentVersion targetVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "版本不存在"));
        
        if (!targetVersion.getDocument().getId().equals(documentId)) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND, "版本不存在");
        }
        
        // 以目标版本内容创建新版本
        CommitDocumentRequest request = new CommitDocumentRequest();
        request.setContent(targetVersion.getContent());
        request.setCommitMessage("回滚到版本 " + targetVersion.getVersionNo());
        
        return commitDocument(documentId, userId, request);
    }
    
    /**
     * 克隆文档
     */
    @Transactional
    public DocumentDTO cloneDocument(Long documentId, Long userId, CloneDocumentRequest request) {
        Document sourceDocument = getDocumentById(documentId);
        
        // 检查是否有查看权限
        if (!checkDocumentAccess(sourceDocument, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        User owner = userService.getUserById(userId);
        
        DocumentFolder folder = null;
        if (request != null && request.getFolderId() != null) {
            folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在"));
            if (!folder.getOwner().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此文件夹");
            }
        }
        
        // 创建克隆文档
        Document clonedDocument = Document.builder()
                .title(sourceDocument.getTitle() + " (克隆)")
                .owner(owner)
                .content(sourceDocument.getContent())
                .docType(sourceDocument.getDocType())
                .visibility("private")
                .tags(sourceDocument.getTags())
                .folder(folder)
                .forkedFrom(sourceDocument)
                .status("active")
                .build();
        
        clonedDocument = documentRepository.save(clonedDocument);
        
        // 创建初始版本
        createInitialVersion(clonedDocument, owner);
        
        return DocumentDTO.fromEntity(clonedDocument, userId, true);
    }
    
    /**
     * 移动文档
     */
    @Transactional
    public DocumentDTO moveDocument(Long documentId, Long userId, MoveDocumentRequest request) {
        Document document = getDocumentById(documentId);
        
        // 检查编辑权限
        if (!checkDocumentEditPermission(document, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权移动此文档");
        }
        
        DocumentFolder targetFolder = null;
        if (request.getTargetFolderId() != null) {
            targetFolder = folderRepository.findById(request.getTargetFolderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "目标文件夹不存在"));
            // 检查目标文件夹是否属于文档所有者
            if (!targetFolder.getOwner().getId().equals(document.getOwner().getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能移动到其他用户的文件夹");
            }
        }
        
        document.setFolder(targetFolder);
        document = documentRepository.save(document);
        
        return DocumentDTO.fromEntity(document, userId, true);
    }
    
    // 辅助方法
    
    private Document getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
    }
    
    private void createInitialVersion(Document document, User user) {
        DocumentVersion initialVersion = DocumentVersion.builder()
                .document(document)
                .versionNo(1)
                .content(document.getContent() != null ? document.getContent() : "")
                .commitMessage("初始版本")
                .createdBy(user)
                .build();
        versionRepository.save(initialVersion);
    }
    
    /**
     * 检查用户是否有文档访问权限
     */
    public boolean checkDocumentAccess(Document document, Long userId) {
        // 所有者可访问
        if (document.getOwner().getId().equals(userId)) {
            return true;
        }
        // 公开文档任何人可访问
        if ("public".equals(document.getVisibility())) {
            return true;
        }
        // 协作者可访问
        return collaboratorRepository.existsByDocumentIdAndUserId(document.getId(), userId);
    }
    
    /**
     * 检查用户是否有文档编辑权限
     */
    public boolean checkDocumentEditPermission(Document document, Long userId) {
        // 所有者可编辑
        if (document.getOwner().getId().equals(userId)) {
            return true;
        }
        // 协作者可编辑
        return collaboratorRepository.existsByDocumentIdAndUserId(document.getId(), userId);
    }
}
