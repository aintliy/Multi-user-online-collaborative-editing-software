package com.example.backend.repository;

import com.example.backend.entity.DocumentWorkspaceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentWorkspaceRequestRepository extends JpaRepository<DocumentWorkspaceRequest, Long> {
    
    List<DocumentWorkspaceRequest> findByDocumentId(Long documentId);
    
    List<DocumentWorkspaceRequest> findByDocumentIdAndStatus(Long documentId, String status);
    
    List<DocumentWorkspaceRequest> findByApplicantId(Long applicantId);
    
    Optional<DocumentWorkspaceRequest> findByDocumentIdAndApplicantIdAndStatus(Long documentId, Long applicantId, String status);
    
    boolean existsByDocumentIdAndApplicantIdAndStatus(Long documentId, Long applicantId, String status);
}
