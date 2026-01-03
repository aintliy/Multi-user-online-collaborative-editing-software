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
    
    List<DocumentWorkspaceRequest> findByDocumentIdAndTypeAndStatus(Long documentId, String type, String status);
    
    List<DocumentWorkspaceRequest> findByApplicantId(Long applicantId);
    
    Optional<DocumentWorkspaceRequest> findByDocumentIdAndApplicantIdAndStatus(Long documentId, Long applicantId, String status);
    
    Optional<DocumentWorkspaceRequest> findByDocumentIdAndApplicantIdAndTypeAndStatus(Long documentId, Long applicantId, String type, String status);
    
    boolean existsByDocumentIdAndApplicantIdAndStatus(Long documentId, Long applicantId, String status);
    
    boolean existsByDocumentIdAndApplicantIdAndTypeAndStatus(Long documentId, Long applicantId, String type, String status);
    
    /**
     * 获取用户拥有的所有文档的待处理协作申请（APPLY类型）
     */
    @Query("SELECT r FROM DocumentWorkspaceRequest r WHERE r.document.owner.id = :ownerId AND r.type = 'APPLY' AND r.status = :status")
    List<DocumentWorkspaceRequest> findByDocumentOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") String status);
    
    /**
     * 获取用户收到的待处理邀请（INVITE类型）
     */
    @Query("SELECT r FROM DocumentWorkspaceRequest r WHERE r.applicant.id = :userId AND r.type = 'INVITE' AND r.status = :status")
    List<DocumentWorkspaceRequest> findPendingInvitesByUserId(@Param("userId") Long userId, @Param("status") String status);
}
