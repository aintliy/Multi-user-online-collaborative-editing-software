package com.example.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.PageResponse;
import com.example.backend.dto.auth.UserDTO;
import com.example.backend.dto.document.DocumentDTO;
import com.example.backend.entity.Document;
import com.example.backend.entity.OperationLog;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.OperationLogRepository;
import com.example.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 管理员服务
 */
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final OperationLogRepository operationLogRepository;
    private final DocumentService documentService;
    
    /**
     * 获取系统统计数据
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("ACTIVEUsers", userRepository.countByStatus("ACTIVE"));
        stats.put("bannedUsers", userRepository.countByStatus("BANNED"));
        stats.put("totalDocuments", documentRepository.count());
        stats.put("publicDocuments", documentRepository.countByVisibility("public"));
        stats.put("privateDocuments", documentRepository.countByVisibility("private"));
        return stats;
    }
    
    /**
     * 获取用户列表
     */
    public PageResponse<UserDTO> getUsers(String keyword, String status, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            userPage = userRepository.searchUsers(keyword, pageRequest);
        } else if (status != null && !status.isEmpty()) {
            userPage = userRepository.findByStatus(status, pageRequest);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }
        
        List<UserDTO> users = userPage.getContent().stream()
                .map(UserDTO::fromEntity)
                .toList();
        
        return new PageResponse<>(users, userPage.getTotalElements(), page, pageSize);
    }
    
    /**
     * 禁用用户
     */
    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "不能禁用管理员");
        }
        
        user.setStatus("BANNED");
        userRepository.save(user);
        
        // 记录操作日志
        logOperation(userId, "BAN_USER", "USER", "禁用用户: " + user.getUsername());
    }
    
    /**
     * 解禁用户
     */
    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        user.setStatus("ACTIVE");
        userRepository.save(user);
        
        // 记录操作日志
        logOperation(userId, "UNBAN_USER", "USER", "解禁用户: " + user.getUsername());
    }
    
    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "不能删除管理员");
        }
        
        // 记录操作日志
        logOperation(userId, "DELETE_USER", "USER", "删除用户: " + user.getUsername());
        
        userRepository.delete(user);
    }
    
    /**
     * 获取文档列表
     */
    public PageResponse<DocumentDTO> getDocuments(String keyword, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> documentPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            documentPage = documentRepository.searchDocuments(keyword, pageRequest);
        } else {
            documentPage = documentRepository.findAll(pageRequest);
        }
        
        List<DocumentDTO> documents = documentPage.getContent().stream()
                .map(doc -> DocumentDTO.fromEntity(doc, null, false))
                .toList();
        
        return new PageResponse<>(documents, documentPage.getTotalElements(), page, pageSize);
    }
    
    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        // 记录操作日志
        logOperation(documentId, "DELETE_DOCUMENT", "DOC", "删除文档: " + document.getTitle());

        documentService.deleteDocument(documentId, document.getOwner().getId(), true);
    }
    
    /**
     * 获取操作日志
     */
    public PageResponse<OperationLog> getOperationLogs(Long userId, String operationType, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OperationLog> logPage;
        
        if (userId != null) {
            logPage = operationLogRepository.findByUserId(userId, pageRequest);
        } else if (operationType != null && !operationType.isEmpty()) {
            logPage = operationLogRepository.findByAction(operationType, pageRequest);
        } else {
            logPage = operationLogRepository.findAll(pageRequest);
        }
        
        return new PageResponse<>(logPage.getContent(), logPage.getTotalElements(), page, pageSize);
    }
    
    /**
     * 记录操作日志
     */
    private void logOperation(Long targetId, String action, String targetType, String detail) {
        OperationLog log = OperationLog.builder()
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .build();
        operationLogRepository.save(log);
    }
}
