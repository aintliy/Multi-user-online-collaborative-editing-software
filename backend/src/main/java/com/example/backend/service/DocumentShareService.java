package com.example.backend.service;

import com.example.backend.dto.document.ShareLinkDTO;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.*;
import com.example.backend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 文档分享服务
 */
@Service
@RequiredArgsConstructor
public class DocumentShareService {
    
    private final DocumentShareLinkRepository shareLinkRepository;
    private final DocumentRepository documentRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserService userService;
    
    /**
     * 创建分享链接（一次性使用）
     */
    @Transactional
    public ShareLinkDTO createShareLink(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        // 只有文档所有者可以创建分享链接
        if (!document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有文档所有者可以创建分享链接");
        }
        
        User user = userService.getUserById(userId);
        
        // 生成唯一token
        String token;
        do {
            token = IdGenerator.generateToken();
        } while (shareLinkRepository.existsByToken(token));
        
        DocumentShareLink shareLink = DocumentShareLink.builder()
                .document(document)
                .token(token)
                .createdBy(user)
                .expiresAt(LocalDateTime.now().plusHours(24))  // 24小时后过期
                .build();
        
        shareLink = shareLinkRepository.save(shareLink);
        
        return ShareLinkDTO.fromEntity(shareLink);
    }
    
    /**
     * 使用分享链接加入协作
     */
    @Transactional
    public Long useShareLink(String token, Long userId) {
        DocumentShareLink shareLink = shareLinkRepository.findValidLink(token, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_LINK_INVALID, "分享链接无效或已过期"));
        
        Document document = shareLink.getDocument();
        User user = userService.getUserById(userId);
        
        // 不能使用自己创建的分享链接
        if (shareLink.getCreatedBy().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能使用自己创建的分享链接");
        }
        
        // 如果是文档所有者，不需要添加为协作者
        if (document.getOwner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "您已是该文档的所有者");
        }
        
        // 检查是否已经是协作者
        if (collaboratorRepository.existsByDocumentIdAndUserId(document.getId(), userId)) {
            throw new BusinessException(ErrorCode.COLLABORATOR_ALREADY_EXISTS, "您已是该文档的协作者");
        }
        
        // 添加为协作者
        DocumentCollaborator collaborator = DocumentCollaborator.builder()
                .document(document)
                .user(user)
                .invitedBy(shareLink.getCreatedBy())
                .build();
        collaboratorRepository.save(collaborator);
        
        // 标记分享链接为已使用
        shareLink.setIsUsed(true);
        shareLink.setUsedBy(user);
        shareLink.setUsedAt(LocalDateTime.now());
        shareLinkRepository.save(shareLink);
        
        return document.getId();
    }
    
    /**
     * 获取分享链接信息（不验证有效性）
     */
    public ShareLinkDTO getShareLinkInfo(String token) {
        DocumentShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_LINK_INVALID, "分享链接不存在"));
        
        return ShareLinkDTO.fromEntity(shareLink);
    }
}
