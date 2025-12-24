package com.example.backend.repository;

import com.example.backend.entity.DocumentInviteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentInviteLinkRepository extends JpaRepository<DocumentInviteLink, Long> {
    
    Optional<DocumentInviteLink> findByToken(String token);
    
    List<DocumentInviteLink> findByDocumentId(Long documentId);
    
    boolean existsByToken(String token);
}
