package com.example.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.DocumentFolder;

@Repository
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {
    
    List<DocumentFolder> findByOwnerId(Long ownerId);
    
    List<DocumentFolder> findByOwnerIdAndParentId(Long ownerId, Long parentId);
    
    List<DocumentFolder> findByOwnerIdAndParentIsNull(Long ownerId);
    
    Optional<DocumentFolder> findByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name);
    
    boolean existsByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name);
    
    boolean existsByOwnerIdAndParentIsNullAndName(Long ownerId, String name);

    @Query("SELECT f FROM DocumentFolder f WHERE f.owner.id = :ownerId AND f.parent.id = f.id")
    Optional<DocumentFolder> findRootFolderByOwnerId(@Param("ownerId") Long ownerId);
}
