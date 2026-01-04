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
    
       @Query("SELECT d FROM Document d WHERE d.visibility = 'PUBLIC' AND d.status <> 'DELETED' AND d.folder IS NOT NULL AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchPublicDocuments(@Param("keyword") String keyword, Pageable pageable);
    
       @Query("SELECT d FROM Document d WHERE d.owner.id = :ownerId AND d.status <> 'DELETED' AND d.folder IS NOT NULL AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchByOwnerAndKeyword(@Param("ownerId") Long ownerId, 
                                            @Param("keyword") String keyword, 
                                            Pageable pageable);
    
    List<Document> findByFolderId(Long folderId);

    List<Document> findByFolderIdAndStatusNot(Long folderId, String status);
    
    @Query("SELECT d FROM Document d WHERE d.owner.id = :ownerId AND d.visibility = 'PUBLIC'")
    Page<Document> findPublicDocumentsByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
    
    /**
     * 查找指定用户的公开文档（按可见性和状态过滤）
     */
    List<Document> findByOwnerIdAndVisibilityAndStatus(Long ownerId, String visibility, String status);
    
       @Query("SELECT COUNT(d) FROM Document d WHERE d.visibility = :visibility AND d.status <> 'DELETED'")
       long countByVisibility(@Param("visibility") String visibility);
    
      @Query("SELECT d FROM Document d WHERE d.status <> 'DELETED' AND d.folder IS NOT NULL AND LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Document> searchDocuments(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 管理员搜索文档（排除已删除）
     */
    @Query("SELECT d FROM Document d WHERE d.status <> 'DELETED' AND LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Document> searchDocumentsExcludeDeleted(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 搜索已删除的文档
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'DELETED' AND LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Document> searchDeletedDocuments(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 按状态查找文档
     */
    Page<Document> findByStatus(String status, Pageable pageable);
    
    /**
     * 按状态排除查找文档
     */
    Page<Document> findByStatusNot(String status, Pageable pageable);
    
    /**
     * 统计非删除状态的文档数
     */
    long countByStatusNot(String status);
    
    /**
     * 统计指定时间后创建的非删除文档数
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.createdAt > :dateTime AND d.status <> :status")
    long countByCreatedAtAfterAndStatusNot(@Param("dateTime") java.time.LocalDateTime dateTime, @Param("status") String status);
    
    /**
     * 检查同一 owner 同一 folder 下是否存在同名文档（排除已删除）
     */
    boolean existsByOwnerIdAndFolderIdAndTitleAndStatusNot(Long ownerId, Long folderId, String title, String status);
    
    /**
     * 检查同一 owner 同一 folder 下是否存在同名文档（排除指定文档和已删除）
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Document d " +
           "WHERE d.owner.id = :ownerId AND d.folder.id = :folderId AND d.title = :title " +
           "AND d.id <> :excludeId AND d.status <> 'DELETED'")
    boolean existsDuplicateTitleExcludingSelf(@Param("ownerId") Long ownerId, 
                                               @Param("folderId") Long folderId, 
                                               @Param("title") String title, 
                                               @Param("excludeId") Long excludeId);
}
