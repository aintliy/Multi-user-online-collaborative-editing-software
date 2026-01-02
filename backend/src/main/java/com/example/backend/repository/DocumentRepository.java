package com.example.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Document;
import com.example.backend.entity.User;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    Page<Document> findByOwner(User owner, Pageable pageable);
    
    Page<Document> findByOwnerId(Long ownerId, Pageable pageable);
    
       Page<Document> findByOwnerIdAndFolderId(Long ownerId, Long folderId, Pageable pageable);

       Page<Document> findByOwnerIdAndFolderIdAndStatusNot(Long ownerId, Long folderId, String status, Pageable pageable);
    
    Page<Document> findByOwnerIdAndFolderIdIsNull(Long ownerId, Pageable pageable);

       Page<Document> findByOwnerIdAndFolderIsNullAndStatusNot(Long ownerId, String status, Pageable pageable);

       Page<Document> findByOwnerIdAndStatusNot(Long ownerId, String status, Pageable pageable);
    
    Page<Document> findByVisibility(String visibility, Pageable pageable);

       Page<Document> findByVisibilityAndStatusNot(String visibility, String status, Pageable pageable);
    
       @Query("SELECT d FROM Document d WHERE d.visibility = 'public' AND d.status <> 'deleted' AND d.folder IS NOT NULL AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchPublicDocuments(@Param("keyword") String keyword, Pageable pageable);
    
       @Query("SELECT d FROM Document d WHERE d.owner.id = :ownerId AND d.status <> 'deleted' AND d.folder IS NOT NULL AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchByOwnerAndKeyword(@Param("ownerId") Long ownerId, 
                                            @Param("keyword") String keyword, 
                                            Pageable pageable);
    
    List<Document> findByFolderId(Long folderId);

    List<Document> findByFolderIdAndStatusNot(Long folderId, String status);
    
    @Query("SELECT d FROM Document d WHERE d.owner.id = :ownerId AND d.visibility = 'public'")
    Page<Document> findPublicDocumentsByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
    
    /**
     * 查找指定用户的公开文档（按可见性和状态过滤）
     */
    List<Document> findByOwnerIdAndVisibilityAndStatus(Long ownerId, String visibility, String status);
    
       @Query("SELECT COUNT(d) FROM Document d WHERE d.visibility = :visibility AND d.status <> 'deleted'")
       long countByVisibility(@Param("visibility") String visibility);
    
      @Query("SELECT d FROM Document d WHERE d.status <> 'deleted' AND d.folder IS NOT NULL AND LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Document> searchDocuments(@Param("keyword") String keyword, Pageable pageable);
}
