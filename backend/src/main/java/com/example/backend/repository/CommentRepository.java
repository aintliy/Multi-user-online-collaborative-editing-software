package com.example.backend.repository;

import com.example.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    Page<Comment> findByDocumentId(Long documentId, Pageable pageable);
    
    List<Comment> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    
    List<Comment> findByReplyToCommentId(Long commentId);
}
