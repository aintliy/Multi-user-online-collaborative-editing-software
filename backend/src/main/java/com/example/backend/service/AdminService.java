package com.example.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.OperationLogDTO;
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
        stats.put("activeUsers", userRepository.countByStatus("ACTIVE"));
        stats.put("bannedUsers", userRepository.countByStatus("BANNED"));
        stats.put("totalDocuments", documentRepository.countByStatusNot("deleted"));
        stats.put("publicDocuments", documentRepository.countByVisibility("public"));
        stats.put("privateDocuments", documentRepository.countByVisibility("private"));
        
        // 今日新建文档数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.put("todayDocuments", documentRepository.countByCreatedAtAfterAndStatusNot(todayStart, "deleted"));
        
        // 最近7天活跃用户数（有登录记录的用户）
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        stats.put("recentActiveUsers", userRepository.countByLastLoginAtAfter(weekAgo));
        
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
     * 获取文档列表（排除已删除的）
     */
    public PageResponse<DocumentDTO> getDocuments(String keyword, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> documentPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            documentPage = documentRepository.searchDocumentsExcludeDeleted(keyword, pageRequest);
        } else {
            documentPage = documentRepository.findByStatusNot("DELETED", pageRequest);
        }
        
        List<DocumentDTO> documents = documentPage.getContent().stream()
                .map(doc -> DocumentDTO.fromEntity(doc, null, false))
                .toList();
        
        return new PageResponse<>(documents, documentPage.getTotalElements(), page, pageSize);
    }
    
    /**
     * 获取回收站文档列表（已被用户逻辑删除的）
     */
    public PageResponse<DocumentDTO> getDeletedDocuments(String keyword, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Document> documentPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            documentPage = documentRepository.searchDeletedDocuments(keyword, pageRequest);
        } else {
            documentPage = documentRepository.findByStatus("DELETED", pageRequest);
        }
        
        List<DocumentDTO> documents = documentPage.getContent().stream()
                .map(doc -> DocumentDTO.fromEntity(doc, null, false))
                .toList();
        
        return new PageResponse<>(documents, documentPage.getTotalElements(), page, pageSize);
    }
    
    /**
     * 恢复已删除的文档
     */
    @Transactional
    public void restoreDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        if (!"DELETED".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "文档未被删除");
        }
        
        document.setStatus("ACTIVE");
        documentRepository.save(document);
        
        // 记录操作日志
        logOperation(documentId, "RESTORE_DOCUMENT", "DOC", "恢复文档: " + document.getTitle());
    }
    
    /**
     * 物理删除文档（永久删除）
     */
    @Transactional
    public void permanentDeleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        // 记录操作日志
        logOperation(documentId, "PERMANENT_DELETE_DOCUMENT", "DOC", "永久删除文档: " + document.getTitle());

        documentService.deleteDocument(documentId, document.getOwner().getId(), true);
    }
    
    /**
     * 删除文档（逻辑删除）
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        // 记录操作日志
        logOperation(documentId, "DELETE_DOCUMENT", "DOC", "删除文档: " + document.getTitle());

        // 逻辑删除
        document.setStatus("DELETED");
        documentRepository.save(document);
    }
    
    /**
     * 获取操作日志
     */
    public PageResponse<OperationLogDTO> getOperationLogs(Long userId, String operationType, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OperationLog> logPage;
        
        if (userId != null) {
            logPage = operationLogRepository.findByUserId(userId, pageRequest);
        } else if (operationType != null && !operationType.isEmpty()) {
            logPage = operationLogRepository.findByAction(operationType, pageRequest);
        } else {
            logPage = operationLogRepository.findAll(pageRequest);
        }
        
        List<OperationLogDTO> logs = logPage.getContent().stream()
                .map(OperationLogDTO::fromEntity)
                .toList();
        
        return new PageResponse<>(logs, logPage.getTotalElements(), page, pageSize);
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
