package com.example.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.entity.DocumentFolder;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.DocumentFolderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件存储服务
 * 统一管理文档物理文件的创建、删除和维护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    /**
     * 物理文件存储根目录，默认为 backend/storage/
     */
    @Value("${file.storage.root:backend/storage}")
    private String storageRoot;

    private final DocumentFolderRepository folderRepository;
    
    /**
     * 为文档创建物理存储目录
     * 
     * @param ownerId 文档所有者ID
     * @param folderId 文档所属文件夹ID
     * @return 相对存储路径，格式: {ownerId}/root/{parentId/.../}folderId/
     */
    public String createDocumentStoragePath(Long ownerId, Long folderId) {
        try {
            String relativePath = buildRelativePath(ownerId, folderId);
            Path fullPath = Paths.get(storageRoot, relativePath);
            
            // 幂等性：如果目录已存在，不会报错
            Files.createDirectories(fullPath);
            
            log.debug("创建文档存储目录: {}", fullPath);
            return relativePath;
        } catch (IOException e) {
            log.error("创建文档存储目录失败: ownerId={}, folderId={}", ownerId, folderId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建文档存储目录失败");
        }
    }
    
    /**
     * 构建相对存储路径
     * 
     * @param ownerId 所有者ID
     * @param folderId 文件夹ID
     * @return 相对路径，格式: {ownerId}/root/{parentId/.../}folderId/
     */
    private String buildRelativePath(Long ownerId, Long folderId) {
        StringBuilder sb = new StringBuilder();
        sb.append(ownerId).append("/");

        // 未指定文件夹则落在 root 下
        if (folderId == null) {
            sb.append("root/");
            return sb.toString();
        }

        List<String> pathSegments = new ArrayList<>();
        DocumentFolder current = folderRepository.findById(folderId).orElse(null);

        int guard = 0;
        while (current != null && guard < 100) {
            pathSegments.add(current.getId().toString());
            DocumentFolder parent = current.getParent();
            if (parent == null || (parent.getId() != null && parent.getId().equals(current.getId()))) {
                break;
            }
            current = parent;
            guard++;
        }

        if (pathSegments.isEmpty()) {
            pathSegments.add(folderId.toString());
        }

        Collections.reverse(pathSegments);
        pathSegments.forEach(seg -> sb.append(seg).append("/"));

        return sb.toString();
    }
    
    /**
     * 保存文件到文档存储目录
     * 
     * @param storagePath 文档存储相对路径
     * @param fileName 文件名
     * @param file 文件对象
     * @return 文件的完整相对路径
     */
    public String saveFile(String storagePath, String fileName, MultipartFile file) {
        try {
            Path directoryPath = Paths.get(storageRoot, storagePath);
            Files.createDirectories(directoryPath);
            
            Path filePath = directoryPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            String fullRelativePath = storagePath + fileName;
            log.debug("保存文件成功: {}", fullRelativePath);
            return fullRelativePath;
        } catch (IOException e) {
            log.error("保存文件失败: storagePath={}, fileName={}", storagePath, fileName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文件失败");
        }
    }
    
    /**
     * 保存字节数组到文档存储目录
     * 
     * @param storagePath 文档存储相对路径
     * @param fileName 文件名
     * @param content 文件内容
     * @return 文件的完整相对路径
     */
    public String saveBytes(String storagePath, String fileName, byte[] content) {
        try {
            Path directoryPath = Paths.get(storageRoot, storagePath);
            Files.createDirectories(directoryPath);
            
            Path filePath = directoryPath.resolve(fileName);
            Files.write(filePath, content);
            
            String fullRelativePath = storagePath + fileName;
            log.debug("保存文件成功: {}", fullRelativePath);
            return fullRelativePath;
        } catch (IOException e) {
            log.error("保存文件失败: storagePath={}, fileName={}", storagePath, fileName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文件失败");
        }
    }
    
    /**
     * 读取文件内容
     * 
     * @param relativePath 文件的完整相对路径
     * @return 文件内容字节数组
     */
    public byte[] readFile(String relativePath) {
        try {
            Path filePath = Paths.get(storageRoot, relativePath);
            if (!Files.exists(filePath)) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取文件失败: {}", relativePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败");
        }
    }
    
    /**
     * 删除文档存储目录及其所有文件
     * 
     * @param storagePath 文档存储相对路径
     */
    public void deleteDocumentStorage(String storagePath) {
        if (storagePath == null || storagePath.trim().isEmpty()) {
            return;
        }
        
        try {
            Path directoryPath = Paths.get(storageRoot, storagePath);
            if (!Files.exists(directoryPath)) {
                log.debug("文档存储目录不存在，无需删除: {}", directoryPath);
                return;
            }
            
            // 递归删除目录及其所有内容
            try (Stream<Path> walk = Files.walk(directoryPath)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
            }
            
            log.debug("删除文档存储目录成功: {}", directoryPath);
        } catch (IOException e) {
            log.error("删除文档存储目录失败: {}", storagePath, e);
            // 删除失败不抛异常，避免影响主流程
        }
    }
    
    /**
     * 删除单个文件
     * 
     * @param relativePath 文件的完整相对路径
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return;
        }
        
        try {
            Path filePath = Paths.get(storageRoot, relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("删除文件成功: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("删除文件失败: {}", relativePath, e);
            // 删除失败不抛异常
        }
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param relativePath 文件的完整相对路径
     * @return 文件是否存在
     */
    public boolean fileExists(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }
        Path filePath = Paths.get(storageRoot, relativePath);
        return Files.exists(filePath);
    }
    
    /**
     * 获取文件的完整物理路径（用于调试）
     * 
     * @param relativePath 相对路径
     * @return 完整物理路径
     */
    public Path getFullPath(String relativePath) {
        return Paths.get(storageRoot, relativePath).toAbsolutePath();
    }
}
