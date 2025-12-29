package com.example.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.dto.PageResponse;
import com.example.backend.dto.document.CloneDocumentRequest;
import com.example.backend.dto.document.CommitDocumentRequest;
import com.example.backend.dto.document.CreateDocumentRequest;
import com.example.backend.dto.document.DocumentDTO;
import com.example.backend.dto.document.DocumentVersionDTO;
import com.example.backend.dto.document.MoveDocumentRequest;
import com.example.backend.dto.document.UpdateDocumentRequest;
import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentFolder;
import com.example.backend.entity.DocumentVersion;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.DocumentCollaboratorRepository;
import com.example.backend.repository.DocumentFolderRepository;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.DocumentVersionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_DELETED = "deleted";
    
    private final DocumentRepository documentRepository;
    private final DocumentFolderRepository folderRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    
    /**
     * 创建文档
     */
    @Transactional
    public DocumentDTO createDocument(Long userId, CreateDocumentRequest request) {
        User owner = userService.getUserById(userId);
        DocumentFolder folder = resolveFolder(userId, request.getFolderId());

        // 创建物理存储目录并获取相对路径
        String storagePath = fileStorageService.createDocumentStoragePath(userId, folder.getId());
        
        Document document = Document.builder()
                .title(request.getTitle())
                .owner(owner)
                .docType(request.getType() != null ? request.getType() : "markdown")
                .visibility(request.getVisibility() != null ? request.getVisibility() : "private")
                .folder(folder)
                .content("")
                .storagePath(storagePath)
                .status(STATUS_ACTIVE)
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
        Document document = getActiveDocument(documentId);
        
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
        Document document = getActiveDocument(documentId);
        
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
        Document document = getActiveDocument(documentId);
        
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
        Document document = getDocumentEntity(documentId);

        // 检查权限：所有者或管理员可删除
        if (!document.getOwner().getId().equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此文档");
        }

        if (isAdmin) {
            if (document.getStoragePath() != null) {
                fileStorageService.deleteDocumentStorage(document.getStoragePath());
            }
            documentRepository.delete(document);
            return;
        }

        if (isDeletedDocument(document)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在或已被删除");
        }

        document.setFolder(null);
        document.setStatus(STATUS_DELETED);
        document.setVisibility("private");
        documentRepository.save(document);
    }
    
    /**
     * 获取文档列表
     */
    public PageResponse<DocumentDTO> getDocuments(Long userId, Long folderId, String keyword, 
                                                   int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Document> documentPage;
        DocumentFolder targetFolder = resolveFolder(userId, folderId);
        
        if (keyword != null && !keyword.isEmpty()) {
            documentPage = documentRepository.searchByOwnerAndKeyword(userId, keyword, pageable);
        } else {
            documentPage = documentRepository.findByOwnerIdAndFolderIdAndStatusNot(userId, targetFolder.getId(), STATUS_DELETED, pageable);
        }
        
        List<DocumentDTO> items = documentPage.getContent().stream()
            .filter(doc -> !isDeletedDocument(doc) && !isFolderDeleted(doc.getFolder()))
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
            documentPage = documentRepository.findByVisibilityAndStatusNot("public", STATUS_DELETED, pageable);
        }
        
        List<DocumentDTO> items = documentPage.getContent().stream()
            .filter(doc -> !isDeletedDocument(doc) && !isFolderDeleted(doc.getFolder()))
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
        Document document = getActiveDocument(documentId);
        
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
        Document document = getActiveDocument(documentId);
        
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
        Document document = getActiveDocument(documentId);
        
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
        Document sourceDocument = getActiveDocument(documentId);
        
        // 检查是否有查看权限
        if (!checkDocumentAccess(sourceDocument, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        User owner = userService.getUserById(userId);
        
        Long folderId = request != null ? request.getFolderId() : null;
        DocumentFolder folder = resolveFolder(userId, folderId);
        
        // 创建物理存储目录并获取相对路径
        String storagePath = fileStorageService.createDocumentStoragePath(userId, folder.getId());
        
        // 创建克隆文档
        Document clonedDocument = Document.builder()
                .title(sourceDocument.getTitle() + " (克隆)")
                .owner(owner)
                .content(sourceDocument.getContent())
                .docType(sourceDocument.getDocType())
                .visibility("private")
                .tags(sourceDocument.getTags())
                .folder(folder)
                .storagePath(storagePath)
                .forkedFrom(sourceDocument)
                .status(STATUS_ACTIVE)
                .build();
        
        clonedDocument = documentRepository.save(clonedDocument);
        
        // 创建初始版本
        createInitialVersion(clonedDocument, owner);
        
        return DocumentDTO.fromEntity(clonedDocument, userId, true);
    }

    /**
     * 导入本地文件为文档
     */
    @Transactional
    public DocumentDTO importDocument(Long userId, Long folderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件为空");
        }

        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "导入文档");
        String extension = getFileExtension(originalFilename);
        if (!isSupportedImportExtension(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持导入 docx、md、markdown、txt 文件");
        }

        DocumentFolder folder = resolveFolder(userId, folderId);
        String docType = determineDocumentType(extension);

        String content;
        try {
            content = extractContentFromFile(file, extension);
        } catch (IOException e) {
            log.error("导入文件读取失败: {}", originalFilename, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败");
        }

        String title = sanitizeFileName(stripExtension(originalFilename, extension));
        if (title.isEmpty()) {
            title = "导入文档";
        }

        String storagePath = fileStorageService.createDocumentStoragePath(userId, folder.getId());
        String storedFileName = sanitizeFileName(originalFilename);
        fileStorageService.saveFile(storagePath, storedFileName, file);

        User owner = userService.getUserById(userId);
        Document document = Document.builder()
                .title(title)
                .owner(owner)
                .content(content)
                .docType(docType)
                .visibility("private")
                .folder(folder)
                .storagePath(storagePath)
                .status(STATUS_ACTIVE)
                .build();
        
        document = documentRepository.save(document);
        createInitialVersion(document, owner);
        
        return DocumentDTO.fromEntity(document, userId, true);
    }
    
    /**
     * 移动文档
     */
    @Transactional
    public DocumentDTO moveDocument(Long documentId, Long userId, MoveDocumentRequest request) {
        Document document = getActiveDocument(documentId);
        
        // 检查编辑权限
        if (!checkDocumentEditPermission(document, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权移动此文档");
        }
        
        DocumentFolder targetFolder = resolveFolder(document.getOwner().getId(), request.getTargetFolderId());
        
        document.setFolder(targetFolder);
        document = documentRepository.save(document);
        
        return DocumentDTO.fromEntity(document, userId, true);
    }
    
    // 辅助方法

    private Document getDocumentEntity(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
    }

    private Document getActiveDocument(Long documentId) {
        Document document = getDocumentEntity(documentId);
        if (isDeletedDocument(document) || isFolderDeleted(document.getFolder())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在或已被删除");
        }
        return document;
    }

    private boolean isDeletedDocument(Document document) {
        return document == null || STATUS_DELETED.equalsIgnoreCase(document.getStatus()) || document.getFolder() == null;
    }

    private boolean isFolderDeleted(DocumentFolder folder) {
        return folder == null || folder.getParent() == null;
    }

    private DocumentFolder resolveFolder(Long userId, Long folderId) {
        if (folderId == null) {
            return getRootFolder(userId);
        }

        DocumentFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在"));

        if (!folder.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此文件夹");
        }

        if (isFolderDeleted(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在或已被删除");
        }

        return folder;
    }

    private DocumentFolder getRootFolder(Long userId) {
        return folderRepository.findRootFolderByOwnerId(userId)
                .orElseGet(() -> recreateRootFolder(userId));
    }

    private DocumentFolder recreateRootFolder(Long userId) {
        List<DocumentFolder> legacyRoots = folderRepository.findByOwnerIdAndParentIsNull(userId);
        if (!legacyRoots.isEmpty()) {
            DocumentFolder root = legacyRoots.get(0);
            root.setParent(root);
            return folderRepository.save(root);
        }

        User owner = userService.getUserById(userId);
        DocumentFolder root = DocumentFolder.builder()
                .owner(owner)
                .name("根目录")
                .parent(null)
                .build();
        root = folderRepository.save(root);
        root.setParent(root);
        return folderRepository.save(root);
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

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "document";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String stripExtension(String filename, String extension) {
        String suffix = "." + extension.toLowerCase();
        if (filename.toLowerCase().endsWith(suffix)) {
            return filename.substring(0, filename.length() - suffix.length());
        }
        return filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isSupportedImportExtension(String extension) {
        return "docx".equals(extension) || "md".equals(extension) || "markdown".equals(extension) || "txt".equals(extension);
    }

    private String determineDocumentType(String extension) {
        return switch (extension) {
            case "docx" -> "docx";
            case "md", "markdown" -> "markdown";
            default -> "txt";
        };
    }

    private String extractContentFromFile(MultipartFile file, String extension) throws IOException {
        return switch (extension) {
            case "docx" -> extractDocxText(file);
            case "md", "markdown", "txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
            default -> new String(file.getBytes(), StandardCharsets.UTF_8);
        };
    }

    private String extractDocxText(MultipartFile file) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : docx.getParagraphs()) {
                sb.append(paragraph.getText()).append("\n");
            }
            return sb.toString().trim();
        }
    }

    /**
     * 检查用户是否有文档访问权限
     */
    public boolean checkDocumentAccess(Document document, Long userId) {
        if (isDeletedDocument(document) || isFolderDeleted(document.getFolder())) {
            return false;
        }

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
        if (isDeletedDocument(document) || isFolderDeleted(document.getFolder())) {
            return false;
        }
        // 所有者可编辑
        if (document.getOwner().getId().equals(userId)) {
            return true;
        }
        // 协作者可编辑
        return collaboratorRepository.existsByDocumentIdAndUserId(document.getId(), userId);
    }
}
