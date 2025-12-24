package com.example.backend.repository;

import com.example.backend.entity.DocumentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {
    
    List<DocumentFolder> findByOwnerId(Long ownerId);
    
    List<DocumentFolder> findByOwnerIdAndParentId(Long ownerId, Long parentId);
    
    List<DocumentFolder> findByOwnerIdAndParentIsNull(Long ownerId);
    
    Optional<DocumentFolder> findByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name);
    
    boolean existsByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name);
    
    boolean existsByOwnerIdAndParentIsNullAndName(Long ownerId, String name);
}
