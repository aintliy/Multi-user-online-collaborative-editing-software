package com.example.backend.repository;

import com.example.backend.entity.DocumentWorkspaceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * 获取用户拥有的所有文档的待处理协作申请
     */
    @Query("SELECT r FROM DocumentWorkspaceRequest r WHERE r.document.owner.id = :ownerId AND r.status = :status")
    List<DocumentWorkspaceRequest> findByDocumentOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") String status);
}
