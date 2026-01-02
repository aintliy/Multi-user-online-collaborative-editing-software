package com.example.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.dto.PageResponse;
import com.example.backend.dto.document.CloneDocumentRequest;
import com.example.backend.dto.document.CommitDocumentRequest;
import com.example.backend.dto.document.CreateDocumentRequest;
import com.example.backend.dto.document.DocumentCacheResponse;
import com.example.backend.dto.document.DocumentDTO;
import com.example.backend.dto.document.DocumentVersionDTO;
import com.example.backend.dto.document.MoveDocumentRequest;
import com.example.backend.dto.document.UpdateDocumentRequest;
import com.example.backend.dto.websocket.WebSocketMessage;
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

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_PUBLIC = "PUBLIC";
    private static final String STATUS_PRIVATE = "PRIVATE";
    
    private final DocumentRepository documentRepository;
    private final DocumentFolderRepository folderRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final CollaborationCacheService collaborationCacheService;
    private final SimpMessagingTemplate messagingTemplate;
    
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
                .docType(normalizeDocType(request.getDocType()))
                .visibility(request.getVisibility() != null ? request.getVisibility() : STATUS_PRIVATE)
                .folder(folder)
                .content("")
                .storagePath(storagePath)
                .status(STATUS_ACTIVE)
                .build();
        
        document = documentRepository.save(document);
        
        // 创建初始版本
        createInitialVersion(document, owner);

        // 同步写入物理文件，便于在存储目录下直接查看
        persistLatestContent(document, document.getContent());
        
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

        // 将最新内容持久化到物理文件
        persistLatestContent(document, request.getContent());
        
        return DocumentVersionDTO.fromEntity(version);
    }

    /**
     * 保存至 Redis 确认态并广播。
     */
    public void saveDocumentCache(Long documentId, Long userId, String username, String content) {
        getEditableDocument(documentId, userId);

        String lockToken = UUID.randomUUID().toString();
        boolean locked = collaborationCacheService.acquireSaveLock(documentId, lockToken);
        if (!locked) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "有人正在保存，请稍后重试");
        }

        try {
            collaborationCacheService.saveConfirmed(documentId, content);
            collaborationCacheService.clearDraft(documentId, userId);

            WebSocketMessage message = WebSocketMessage.builder()
                    .type("SAVE_CONFIRMED")
                    .documentId(documentId)
                    .userId(userId)
                    .nickname(username)
                    .data(Map.of("content", content))
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
        } finally {
            collaborationCacheService.releaseSaveLock(documentId, lockToken);
        }
    }

    /**
     * 使用 Redis confirmed 内容提交版本（保持现有提交语义）。
     */
    @Transactional
    public DocumentVersionDTO commitDocumentFromCache(Long documentId, Long userId, String commitMessage) {
        Document document = getEditableDocument(documentId, userId);
        String cached = collaborationCacheService.getConfirmed(documentId);
        String content = cached != null ? cached : (document.getContent() == null ? "" : document.getContent());

        CommitDocumentRequest request = new CommitDocumentRequest();
        request.setContent(content);
        request.setCommitMessage(commitMessage);

        DocumentVersionDTO version = commitDocument(documentId, userId, request);
        collaborationCacheService.clearContentCaches(documentId);
        return version;
    }

    /**
     * 获取协作缓存状态（确认态 + 当前用户草稿 + 在线列表）。
     */
    public DocumentCacheResponse getDocumentCache(Long documentId, Long userId) {
        Document document = getActiveDocument(documentId);
        if (!checkDocumentAccess(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }

        String confirmed = collaborationCacheService.getConfirmed(documentId);
        if (confirmed == null) {
            confirmed = document.getContent() == null ? "" : document.getContent();
            collaborationCacheService.saveConfirmed(documentId, confirmed);
        }

        return DocumentCacheResponse.builder()
                .confirmedContent(confirmed)
                .userDraftContent(collaborationCacheService.getDraft(documentId, userId))
                .onlineUsers(collaborationCacheService.getOnlineUsers(documentId))
            .draftTtlSeconds(collaborationCacheService.getDraftTtlSeconds(documentId, userId))
                .build();
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
                fileStorageService.deleteFile(document.getStoragePath());
            }
            collaborationCacheService.clearDocumentState(documentId);
            documentRepository.delete(document);
            return;
        }

        if (isDeletedDocument(document)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在或已被删除");
        }

        document.setFolder(null);
        document.setStatus(STATUS_DELETED);
        document.setVisibility(STATUS_PRIVATE);
        documentRepository.save(document);

        // 清理协作缓存（confirmed/draft/online）
        collaborationCacheService.clearDocumentState(documentId);
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
            documentPage = documentRepository.findByVisibilityAndStatusNot(STATUS_PUBLIC, STATUS_DELETED, pageable);
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
     * 回滚文档版本（只有所有者可以回滚）
     */
    @Transactional
    public DocumentVersionDTO rollbackVersion(Long documentId, Long versionId, Long userId) {
        Document document = getActiveDocument(documentId);
        
        // 只有所有者可以回滚版本
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以回滚版本");
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

        DocumentVersionDTO result = commitDocument(documentId, userId, request);

        // 刷新协作缓存：清空草稿，确认态回填回滚内容
        collaborationCacheService.clearAllDrafts(documentId);
        collaborationCacheService.saveConfirmed(documentId, targetVersion.getContent());

        // 广播确认内容，让在线用户立即看到回滚结果
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SAVE_CONFIRMED")
            .documentId(documentId)
            .userId(userId)
            .nickname(userService.getUserById(userId).getUsername())
            .data(Map.of("content", targetVersion.getContent()))
            .timestamp(System.currentTimeMillis())
            .build();
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);

        return result;
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
                .docType(normalizeDocType(sourceDocument.getDocType()))
                .visibility(STATUS_PRIVATE)
                .tags(sourceDocument.getTags())
                .folder(folder)
                .storagePath(storagePath)
                .forkedFrom(sourceDocument)
                .status(STATUS_ACTIVE)
                .build();
        
        clonedDocument = documentRepository.save(clonedDocument);
        
        // 创建初始版本
        createInitialVersion(clonedDocument, owner);

        // 写入克隆后的内容
        persistLatestContent(clonedDocument, clonedDocument.getContent());
        
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
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持导入 markdown(md) 或 txt 文件");
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
                .visibility(STATUS_PRIVATE)
                .folder(folder)
                .storagePath(storagePath)
                .status(STATUS_ACTIVE)
                .build();
        
        document = documentRepository.save(document);
        createInitialVersion(document, owner);

        // 写入导入的内容到物理文件
        persistLatestContent(document, content);
        
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

    /**
     * 校验编辑权限并返回文档实体。
     */
    @Transactional
    public Document getEditableDocument(Long documentId, Long userId) {
        Document document = getActiveDocument(documentId);
        if (!checkDocumentEditPermission(document, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此文档");
        }
        return document;
    }

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
        return folder == null || STATUS_DELETED.equalsIgnoreCase(folder.getStatus());
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
            if (root.getStatus() == null) {
                root.setStatus(STATUS_ACTIVE);
            }
            return folderRepository.save(root);
        }

        User owner = userService.getUserById(userId);
        DocumentFolder root = DocumentFolder.builder()
                .owner(owner)
                .name("根目录")
                .parent(null)
                .status(STATUS_ACTIVE)
                .build();
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
        return "md".equals(extension) || "markdown".equals(extension) || "txt".equals(extension);
    }

    private String determineDocumentType(String extension) {
        return switch (extension) {
            case "md", "markdown" -> "markdown";
            case "txt" -> "txt";
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持导入 markdown(md) 或 txt 文件");
        };
    }

    private String extractContentFromFile(MultipartFile file, String extension) throws IOException {
        return switch (extension) {
            case "md", "markdown", "txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持导入 markdown(md) 或 txt 文件");
        };
    }

    private String normalizeDocType(String docType) {
        if (docType == null || docType.isEmpty()) {
            return "markdown";
        }
        String lowered = docType.toLowerCase();
        return switch (lowered) {
            case "md", "markdown" -> "markdown";
            case "txt", "text", "plaintext" -> "txt";
            case "docx" -> "markdown"; // 旧数据降级为 markdown
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "文档类型仅支持 markdown 或 txt");
        };
    }

    /**
     * 检查用户是否有文档访问权限
     */
    public boolean checkDocumentAccess(Document document, Long userId) {
        if (isDeletedDocument(document) || isFolderDeleted(document.getFolder())) {
            return false;
        }

        // 系统管理员允许预览
        if (isAdminUser(userId)) {
            return true;
        }

        // 所有者可访问
        if (document.getOwner().getId().equals(userId)) {
            return true;
        }
        // 公开文档任何人可访问
        if (STATUS_PUBLIC.equalsIgnoreCase(document.getVisibility())) {
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

    /**
     * 将最新内容写入存储目录，便于直接在 storage 目录查看落盘文件。
     */
    private void persistLatestContent(Document document, String content) {
        if (document == null || document.getStoragePath() == null || document.getStoragePath().isBlank()) {
            return;
        }

        String extension = "txt";
        String docType = document.getDocType();
        if (docType != null && (docType.equalsIgnoreCase("markdown") || docType.equalsIgnoreCase("md"))) {
            extension = "md";
        }

        String fileName = document.getTitle() + "." + extension;
        try {
            byte[] bytes = content != null ? content.getBytes(StandardCharsets.UTF_8) : new byte[0];
            fileStorageService.saveBytes(document.getStoragePath(), fileName, bytes);
        } catch (Exception e) {
            log.warn("写入文档物理文件失败 documentId={}", document != null ? document.getId() : null, e);
        }
    }

    private boolean isAdminUser(Long userId) {
        try {
            User user = userService.getUserById(userId);
            return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取用户的公开文档列表
     */
    public List<DocumentDTO> getUserPublicDocuments(Long userId, Long currentUserId) {
        return documentRepository.findByOwnerIdAndVisibilityAndStatus(userId, STATUS_PUBLIC, STATUS_ACTIVE)
                .stream()
                .map(doc -> DocumentDTO.fromEntity(doc, currentUserId, false))  // 公开文档默认不可编辑
                .collect(java.util.stream.Collectors.toList());
    }
}
