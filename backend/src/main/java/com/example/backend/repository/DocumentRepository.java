package com.example.backend.repository;

import com.example.backend.entity.Document;
import com.example.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    Page<Document> findByOwner(User owner, Pageable pageable);
    
    Page<Document> findByOwnerId(Long ownerId, Pageable pageable);
    
    Page<Document> findByOwnerIdAndFolderId(Long ownerId, Long folderId, Pageable pageable);
    
    Page<Document> findByOwnerIdAndFolderIdIsNull(Long ownerId, Pageable pageable);
    
    Page<Document> findByVisibility(String visibility, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.visibility = 'public' AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchPublicDocuments(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.owner.id = :ownerId AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchByOwnerAndKeyword(@Param("ownerId") Long ownerId, 
                                            @Param("keyword") String keyword, 
                                            Pageable pageable);
    
    List<Document> findByFolderId(Long folderId);
    
    @Query("SELECT d FROM Document d WHERE d.owner.id = :ownerId AND d.visibility = 'public'")
    Page<Document> findPublicDocumentsByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
    
    long countByVisibility(String visibility);
    
    @Query("SELECT d FROM Document d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Document> searchDocuments(@Param("keyword") String keyword, Pageable pageable);
}
