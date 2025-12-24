package com.example.backend.repository;

import com.example.backend.entity.DocumentCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentCollaboratorRepository extends JpaRepository<DocumentCollaborator, Long> {
    
    List<DocumentCollaborator> findByDocumentId(Long documentId);
    
    List<DocumentCollaborator> findByUserId(Long userId);
    
    Optional<DocumentCollaborator> findByDocumentIdAndUserId(Long documentId, Long userId);
    
    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);
    
    void deleteByDocumentIdAndUserId(Long documentId, Long userId);
}
