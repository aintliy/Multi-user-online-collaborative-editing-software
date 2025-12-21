package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CreateDocumentRequest;
import com.example.demo.dto.DocumentVO;
import com.example.demo.dto.ShareDocumentRequest;
import com.example.demo.dto.UpdateDocumentRequest;
import com.example.demo.entity.Document;
import com.example.demo.entity.DocumentPermission;
import com.example.demo.entity.DocumentVersion;
import com.example.demo.entity.User;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.DocumentPermissionMapper;
import com.example.demo.mapper.DocumentVersionMapper;
import com.example.demo.mapper.UserMapper;

/**
 * 文档服务
 */
@Service
public class DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentVersionMapper versionMapper;

    @Autowired
    private DocumentPermissionMapper permissionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 创建文档
     */
    @Transactional
    public DocumentVO createDocument(Long userId, CreateDocumentRequest request) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setOwnerId(userId);
        document.setContent(request.getContent() != null ? request.getContent() : "");
        document.setDocType(request.getDocType() != null ? request.getDocType() : "doc");
        document.setStatus("active");
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        documentMapper.insert(document);

        // 创建初始版本
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(document.getId());
        version.setVersionNo(1);
        version.setContent(document.getContent());
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(version);

        // 记录操作日志
        operationLogService.log(userId, "CREATE_DOC", "DOC", document.getId(), 
                                "创建文档：" + document.getTitle());

        return convertToVO(document, userId);
    }

    /**
     * 获取文档列表（分页，支持高级搜索）
     */
    public IPage<DocumentVO> getDocumentList(Long userId, int pageNum, int pageSize, 
                                              String keyword, String type, String tags,
                                              Long ownerId, String startDate, String endDate,
                                              String sortBy, String sortOrder) {
        Page<Document> page = new Page<>(pageNum, pageSize);

        // 查询用户拥有的文档 + 有权限访问的文档
        List<Long> accessibleIds = getAccessibleDocumentIds(userId);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getStatus, "active")
               .and(w -> w.eq(Document::getOwnerId, userId)
                          .or()
                          .in(!accessibleIds.isEmpty(), Document::getId, accessibleIds));
        
        // 关键字搜索：标题和内容
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Document::getTitle, keyword)
                              .or()
                              .like(Document::getContent, keyword));
        }
        
        // 文档类型筛选
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Document::getDocType, type);
        }
        
        // 标签筛选（支持多标签，用逗号分隔）
        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                wrapper.like(Document::getTags, tag.trim());
            }
        }
        
        // 作者筛选
        if (ownerId != null) {
            wrapper.eq(Document::getOwnerId, ownerId);
        }
        
        // 日期范围筛选
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Document::getUpdatedAt, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Document::getUpdatedAt, endDate);
        }
        
        // 排序（支持按标题、创建时间、更新时间排序）
        if ("asc".equalsIgnoreCase(sortOrder)) {
            if ("title".equals(sortBy)) {
                wrapper.orderByAsc(Document::getTitle);
            } else if ("createdAt".equals(sortBy)) {
                wrapper.orderByAsc(Document::getCreatedAt);
            } else {
                wrapper.orderByAsc(Document::getUpdatedAt);
            }
        } else {
            if ("title".equals(sortBy)) {
                wrapper.orderByDesc(Document::getTitle);
            } else if ("createdAt".equals(sortBy)) {
                wrapper.orderByDesc(Document::getCreatedAt);
            } else {
                wrapper.orderByDesc(Document::getUpdatedAt);
            }
        }

        IPage<Document> documentPage = documentMapper.selectPage(page, wrapper);

        // 转换为 VO
        IPage<DocumentVO> voPage = new Page<>(documentPage.getCurrent(), documentPage.getSize(), documentPage.getTotal());
        List<DocumentVO> voList = documentPage.getRecords().stream()
                .map(doc -> convertToVO(doc, userId))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 获取文档详情
     */
    public DocumentVO getDocumentById(Long userId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 检查权限
        if (!hasPermission(userId, documentId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }

        return convertToVO(document, userId);
    }

    /**
     * 更新文档
     */
    @Transactional
    public DocumentVO updateDocument(Long userId, Long documentId, UpdateDocumentRequest request) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 检查编辑权限
        String permission = getPermission(userId, documentId);
        if (!"OWNER".equals(permission) && !"EDITOR".equals(permission)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION, "无编辑权限");
        }

        // 更新文档
        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());

            // 创建新版本
            LambdaQueryWrapper<DocumentVersion> versionWrapper = new LambdaQueryWrapper<>();
            versionWrapper.eq(DocumentVersion::getDocumentId, documentId)
                         .orderByDesc(DocumentVersion::getVersionNo)
                         .last("LIMIT 1");
            DocumentVersion lastVersion = versionMapper.selectOne(versionWrapper);
            int nextVersionNo = lastVersion != null ? lastVersion.getVersionNo() + 1 : 1;

            DocumentVersion newVersion = new DocumentVersion();
            newVersion.setDocumentId(documentId);
            newVersion.setVersionNo(nextVersionNo);
            newVersion.setContent(request.getContent());
            newVersion.setCreatedBy(userId);
            newVersion.setCreatedAt(LocalDateTime.now());
            versionMapper.insert(newVersion);
        }
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        return convertToVO(document, userId);
    }

    /**
     * 删除文档
     */
    public void deleteDocument(Long userId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 只有所有者可以删除
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION, "只有所有者可以删除文档");
        }

        // 软删除
        document.setStatus("deleted");
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        // 记录操作日志
        operationLogService.log(userId, "DELETE_DOC", "DOC", documentId, 
                                "删除文档：" + document.getTitle());
    }

    /**
     * 分享文档给其他用户
     */
    @Transactional
    public void shareDocument(Long userId, ShareDocumentRequest request) {
        Document document = documentMapper.selectById(request.getDocumentId());
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 只有所有者可以分享
        if (!document.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION, "只有所有者可以分享文档");
        }

        // 查询目标用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getEmail, request.getUserEmail());
        User targetUser = userMapper.selectOne(userWrapper);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查是否已有权限
        LambdaQueryWrapper<DocumentPermission> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(DocumentPermission::getDocumentId, request.getDocumentId())
                   .eq(DocumentPermission::getUserId, targetUser.getId());
        DocumentPermission existing = permissionMapper.selectOne(permWrapper);

        if (existing != null) {
            // 更新权限
            existing.setRole(request.getRole());
            permissionMapper.updateById(existing);
        } else {
            // 新增权限
            DocumentPermission permission = new DocumentPermission();
            permission.setDocumentId(request.getDocumentId());
            permission.setUserId(targetUser.getId());
            permission.setRole(request.getRole());
            permission.setCreatedAt(LocalDateTime.now());
            permissionMapper.insert(permission);
        }

        // 记录操作日志
        operationLogService.log(userId, "UPDATE_PERMISSION", "DOC", request.getDocumentId(), 
                                "分享文档给用户 " + request.getUserEmail() + "，权限：" + request.getRole());
    }

    /**
     * 获取文档版本列表
     */
    public List<DocumentVersion> getVersions(Long userId, Long documentId) {
        // 检查权限
        if (!hasPermission(userId, documentId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }

        LambdaQueryWrapper<DocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentVersion::getDocumentId, documentId)
               .orderByDesc(DocumentVersion::getVersionNo);

        return versionMapper.selectList(wrapper);
    }

    /**
     * 获取文档版本详情
     */
    public DocumentVersion getVersionDetail(Long userId, Long documentId, Long versionId) {
        // 检查权限
        if (!hasPermission(userId, documentId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }

        DocumentVersion version = versionMapper.selectById(versionId);
        if (version == null || !version.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "版本不存在");
        }

        return version;
    }

    /**
     * 回滚到指定版本
     */
    @Transactional
    public DocumentVO rollbackVersion(Long userId, Long documentId, Long versionId) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(documentId);
        if (document == null || "deleted".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 检查编辑权限
        String permission = getPermission(userId, documentId);
        if (!"OWNER".equals(permission) && !"EDITOR".equals(permission)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_PERMISSION);
        }

        // 获取指定版本
        DocumentVersion targetVersion = versionMapper.selectById(versionId);
        if (targetVersion == null || !targetVersion.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "版本不存在");
        }

        // 将该版本内容设为当前内容
        document.setContent(targetVersion.getContent());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        // 创建新版本记录（记录回滚操作）
        LambdaQueryWrapper<DocumentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(DocumentVersion::getDocumentId, documentId)
                     .orderByDesc(DocumentVersion::getVersionNo)
                     .last("LIMIT 1");
        DocumentVersion lastVersion = versionMapper.selectOne(versionWrapper);
        int nextVersionNo = lastVersion != null ? lastVersion.getVersionNo() + 1 : 1;

        DocumentVersion newVersion = new DocumentVersion();
        newVersion.setDocumentId(documentId);
        newVersion.setVersionNo(nextVersionNo);
        newVersion.setContent(targetVersion.getContent());
        newVersion.setCreatedBy(userId);
        newVersion.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(newVersion);

        return convertToVO(document, userId);
    }

    /**
     * 检查用户是否有权限访问文档
     */
    private boolean hasPermission(Long userId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            return false;
        }

        // 所有者
        if (document.getOwnerId().equals(userId)) {
            return true;
        }

        // 检查共享权限
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId)
               .eq(DocumentPermission::getUserId, userId);
        return permissionMapper.selectOne(wrapper) != null;
    }

    /**
     * 获取用户对文档的权限
     */
    private String getPermission(Long userId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            return null;
        }

        // 所有者
        if (document.getOwnerId().equals(userId)) {
            return "OWNER";
        }

        // 检查共享权限
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId)
               .eq(DocumentPermission::getUserId, userId);
        DocumentPermission permission = permissionMapper.selectOne(wrapper);

        return permission != null ? permission.getRole() : null;
    }

    /**
     * 获取用户可访问的文档 ID 列表
     */
    private List<Long> getAccessibleDocumentIds(Long userId) {
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getUserId, userId);
        List<DocumentPermission> permissions = permissionMapper.selectList(wrapper);
        return permissions.stream()
                .map(DocumentPermission::getDocumentId)
                .collect(Collectors.toList());
    }

    /**
     * 实体转 VO
     */
    private DocumentVO convertToVO(Document document, Long currentUserId) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);

        // 设置所有者用户名
        User owner = userMapper.selectById(document.getOwnerId());
        if (owner != null) {
            vo.setOwnerName(owner.getUsername());
        }

        // 设置当前用户的权限
        vo.setPermission(getPermission(currentUserId, document.getId()));

        return vo;
    }
}
