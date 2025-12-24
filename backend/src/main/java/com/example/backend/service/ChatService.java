package com.example.backend.service;

import com.example.backend.dto.PageResponse;
import com.example.backend.entity.ChatMessage;
import com.example.backend.entity.Document;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.ChatMessageRepository;
import com.example.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天服务
 */
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final UserService userService;
    private final DocumentService documentService;
    
    /**
     * 保存聊天消息
     */
    @Transactional
    public ChatMessage saveMessage(Long documentId, Long senderId, String content) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        User sender = userService.getUserById(senderId);
        
        ChatMessage message = ChatMessage.builder()
                .document(document)
                .sender(sender)
                .content(content)
                .build();
        
        return chatMessageRepository.save(message);
    }
    
    /**
     * 获取聊天历史
     */
    public PageResponse<Map<String, Object>> getChatHistory(Long documentId, Long userId, int page, int pageSize) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        
        if (!documentService.checkDocumentAccess(document, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED, "无权访问此文档");
        }
        
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<ChatMessage> messagePage = chatMessageRepository.findByDocumentIdOrderByCreatedAtDesc(documentId, pageable);
        
        List<Map<String, Object>> items = messagePage.getContent().stream()
                .map(msg -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", msg.getId());
                    item.put("documentId", msg.getDocument().getId());
                    item.put("senderId", msg.getSender().getId());
                    item.put("senderName", msg.getSender().getUsername());
                    item.put("content", msg.getContent());
                    item.put("createdAt", msg.getCreatedAt());
                    return item;
                })
                .collect(Collectors.toList());
        
        return PageResponse.<Map<String, Object>>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .total(messagePage.getTotalElements())
                .build();
    }
}
