package com.example.backend.service;

import com.example.backend.dto.folder.CreateFolderRequest;
import com.example.backend.dto.folder.FolderDTO;
import com.example.backend.dto.folder.UpdateFolderRequest;
import com.example.backend.entity.DocumentFolder;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.DocumentFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件夹服务
 */
@Service
@RequiredArgsConstructor
public class FolderService {
    
    private final DocumentFolderRepository folderRepository;
    private final UserService userService;
    
    /**
     * 获取用户的文件夹树
     */
    public List<FolderDTO> getFolderTree(Long userId) {
        List<DocumentFolder> allFolders = folderRepository.findByOwnerId(userId);
        
        // 构建树形结构
        Map<Long, List<DocumentFolder>> childrenMap = allFolders.stream()
                .filter(f -> f.getParent() != null)
                .collect(Collectors.groupingBy(f -> f.getParent().getId()));
        
        // 获取根目录下的文件夹
        List<DocumentFolder> rootFolders = allFolders.stream()
                .filter(f -> f.getParent() == null)
                .collect(Collectors.toList());
        
        return rootFolders.stream()
                .map(folder -> buildFolderTree(folder, childrenMap))
                .collect(Collectors.toList());
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
    
    /**
     * 创建文件夹
     */
    @Transactional
    public FolderDTO createFolder(Long userId, CreateFolderRequest request) {
        User owner = userService.getUserById(userId);
        
        // 检查同一父目录下是否已存在同名文件夹
        if (request.getParentId() != null) {
            if (folderRepository.existsByOwnerIdAndParentIdAndName(userId, request.getParentId(), request.getName())) {
                throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATE, "同一目录下文件夹名称不能重复");
            }
        } else {
            if (folderRepository.existsByOwnerIdAndParentIsNullAndName(userId, request.getName())) {
                throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATE, "同一目录下文件夹名称不能重复");
            }
        }
        
        DocumentFolder parent = null;
        if (request.getParentId() != null) {
            parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND, "父文件夹不存在"));
            // 验证父文件夹是否属于当前用户
            if (!parent.getOwner().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此文件夹");
            }
        }
        
        DocumentFolder folder = DocumentFolder.builder()
                .owner(owner)
                .name(request.getName())
                .parent(parent)
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
        
        // 检查同一父目录下是否已存在同名文件夹
        Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
        if (parentId != null) {
            if (folderRepository.existsByOwnerIdAndParentIdAndName(userId, parentId, request.getName())) {
                // 排除自己
                DocumentFolder existing = folderRepository.findByOwnerIdAndParentIdAndName(userId, parentId, request.getName()).orElse(null);
                if (existing != null && !existing.getId().equals(folderId)) {
                    throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATE, "同一目录下文件夹名称不能重复");
                }
            }
        } else {
            if (folderRepository.existsByOwnerIdAndParentIsNullAndName(userId, request.getName())) {
                DocumentFolder existing = folderRepository.findByOwnerIdAndParentIdAndName(userId, null, request.getName()).orElse(null);
                if (existing != null && !existing.getId().equals(folderId)) {
                    throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATE, "同一目录下文件夹名称不能重复");
                }
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
        
        // 删除文件夹（子文件夹会级联删除，文档的folder_id会被设为NULL）
        folderRepository.delete(folder);
    }
}
