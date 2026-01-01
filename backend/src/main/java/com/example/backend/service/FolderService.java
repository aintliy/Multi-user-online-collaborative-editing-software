package com.example.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.folder.CreateFolderRequest;
import com.example.backend.dto.folder.FolderDTO;
import com.example.backend.dto.folder.UpdateFolderRequest;
import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentFolder;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.DocumentFolderRepository;
import com.example.backend.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 文件夹服务
 */
@Service
@RequiredArgsConstructor
public class FolderService {
    
    private final DocumentFolderRepository folderRepository;
    private final UserService userService;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final CollaborationCacheService collaborationCacheService;
    
    /**
     * 获取用户的文件夹树
     */
    public List<FolderDTO> getFolderTree(Long userId) {
        DocumentFolder root = getOrCreateRootFolder(userId);
        List<DocumentFolder> allFolders = folderRepository.findByOwnerId(userId).stream()
            // 过滤掉已逻辑删除的文件夹
            .filter(f -> !isDeletedFolder(f))
            .filter(f -> f.getParent() != null)
            .collect(Collectors.toList());

        // 构建树形结构，忽略自引用节点以避免循环
        Map<Long, List<DocumentFolder>> childrenMap = allFolders.stream()
            .filter(f -> f.getParent() != null && !f.getParent().getId().equals(f.getId()))
            .collect(Collectors.groupingBy(f -> f.getParent().getId()));

        FolderDTO rootDto = buildFolderTree(root, childrenMap);
        return List.of(rootDto);
    }
    
    private FolderDTO buildFolderTree(DocumentFolder folder, Map<Long, List<DocumentFolder>> childrenMap) {
        FolderDTO dto = FolderDTO.fromEntity(folder);
        
        List<DocumentFolder> children = childrenMap.get(folder.getId());
        if (children != null && !children.isEmpty()) {
            dto.setChildren(children.stream()
                    .map(child -> buildFolderTree(child, childrenMap))
                    .collect(Collectors.toList()));
        } else {
            dto.setChildren(new ArrayList<>());
        }
        
        return dto;
    }

    private DocumentFolder resolveParentFolder(Long userId, Long parentId) {
        if (parentId == null) {
            return getOrCreateRootFolder(userId);
        }

        DocumentFolder parent = folderRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "父文件夹不存在"));

        if (!parent.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此文件夹");
        }

        if (isDeletedFolder(parent)) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "父文件夹不存在或已被删除");
        }

        return parent;
    }

    private DocumentFolder getOrCreateRootFolder(Long userId) {
        return folderRepository.findRootFolderByOwnerId(userId)
                .orElseGet(() -> fixLegacyRootFolder(userId));
    }

    private DocumentFolder fixLegacyRootFolder(Long userId) {
        List<DocumentFolder> legacyRoots = folderRepository.findByOwnerIdAndParentIsNull(userId);
        if (!legacyRoots.isEmpty()) {
            DocumentFolder root = legacyRoots.get(0);
            if (root.getStatus() == null) {
                root.setStatus("ACTIVE");
            }
            return folderRepository.save(root);
        }

        User owner = userService.getUserById(userId);
        DocumentFolder root = DocumentFolder.builder()
                .owner(owner)
                .name("根目录")
                .parent(null)
                .status("ACTIVE")
                .build();
        return folderRepository.save(root);
    }

    private boolean isRootFolder(DocumentFolder folder) {
        return folder.getParent() == null && "根目录".equals(folder.getName());
    }

    private boolean isDeletedFolder(DocumentFolder folder) {
        return "deleted".equalsIgnoreCase(folder.getStatus());
    }

    private List<DocumentFolder> collectFolderSubtree(DocumentFolder folder) {
        List<DocumentFolder> result = new ArrayList<>();
        result.add(folder);

        List<DocumentFolder> children = folderRepository.findByOwnerIdAndParentId(folder.getOwner().getId(), folder.getId());
        for (DocumentFolder child : children) {
            // 跳过自引用节点（根目录）避免递归死循环
            if (child.getId().equals(child.getParent().getId())) {
                continue;
            }
            result.addAll(collectFolderSubtree(child));
        }

        return result;
    }

    private void logicalDeleteFolder(DocumentFolder folder) {
        List<DocumentFolder> subtree = collectFolderSubtree(folder);

        for (DocumentFolder current : subtree) {
            markDocumentsDeleted(current.getId());
            current.setParent(null);
            current.setStatus("deleted");
        }

        folderRepository.saveAll(subtree);
    }

    private void markDocumentsDeleted(Long folderId) {
        List<Document> documents = documentRepository.findByFolderIdAndStatusNot(folderId, "deleted");
        if (documents.isEmpty()) {
            return;
        }

        documents.forEach(doc -> {
            doc.setFolder(null);
            doc.setStatus("deleted");
            doc.setVisibility("private");
            collaborationCacheService.clearDocumentState(doc.getId());
        });

        documentRepository.saveAll(documents);
    }

    private void physicalDeleteFolder(DocumentFolder folder) {
        List<DocumentFolder> subtree = collectFolderSubtree(folder);

        for (DocumentFolder current : subtree) {
            List<Document> documents = documentRepository.findByFolderId(current.getId());
            for (Document document : documents) {
                if (document.getStoragePath() != null) {
                    fileStorageService.deleteDocumentStorage(document.getStoragePath());
                }
                collaborationCacheService.clearDocumentState(document.getId());
                documentRepository.delete(document);
            }
        }

        List<DocumentFolder> toDelete = new ArrayList<>(subtree);
        Collections.reverse(toDelete);
        toDelete.forEach(folderRepository::delete);
    }
    
    /**
     * 创建文件夹
     */
    @Transactional
    public FolderDTO createFolder(Long userId, CreateFolderRequest request) {
        User owner = userService.getUserById(userId);
        DocumentFolder parent = resolveParentFolder(userId, request.getParentId());

        // 检查同一父目录下是否已存在同名文件夹
        Long parentId = parent != null ? parent.getId() : null;
        if (parentId != null && folderRepository.existsByOwnerIdAndParentIdAndName(userId, parentId, request.getName())) {
            throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATE, "同一目录下文件夹名称不能重复");
        }
        
        DocumentFolder folder = DocumentFolder.builder()
                .owner(owner)
                .name(request.getName())
            .parent(parent)
            .status("ACTIVE")
                .build();
        
        folder = folderRepository.save(folder);
        return FolderDTO.fromEntity(folder);
    }
    
    /**
     * 重命名文件夹
     */
    @Transactional
    public FolderDTO updateFolder(Long folderId, Long userId, UpdateFolderRequest request) {
        DocumentFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在"));
        
        // 验证权限
        if (!folder.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此文件夹");
        }

        if (isDeletedFolder(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在或已被删除");
        }
        
        // 检查同一父目录下是否已存在同名文件夹
        Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
        if (parentId != null) {
            DocumentFolder existing = folderRepository.findByOwnerIdAndParentIdAndName(userId, parentId, request.getName()).orElse(null);
            if (existing != null && !existing.getId().equals(folderId)) {
                throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATE, "同一目录下文件夹名称不能重复");
            }
        }
        
        folder.setName(request.getName());
        folder = folderRepository.save(folder);
        return FolderDTO.fromEntity(folder);
    }
    
    /**
     * 删除文件夹
     */
    @Transactional
    public void deleteFolder(Long folderId, Long userId, boolean isAdmin) {
        DocumentFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在"));
        
        // 验证权限
        if (!folder.getOwner().getId().equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除此文件夹");
        }

        if (isRootFolder(folder)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "根目录不可删除");
        }

        if (isAdmin) {
            physicalDeleteFolder(folder);
        } else {
            if (isDeletedFolder(folder)) {
                throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "文件夹不存在或已被删除");
            }
            logicalDeleteFolder(folder);
        }
    }
}
