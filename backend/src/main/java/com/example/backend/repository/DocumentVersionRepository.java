package com.example.backend.repository;

import com.example.backend.entity.DocumentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    
    Page<DocumentVersion> findByDocumentIdOrderByVersionNoDesc(Long documentId, Pageable pageable);
    
    Optional<DocumentVersion> findByDocumentIdAndVersionNo(Long documentId, Integer versionNo);
    
    @Query("SELECT MAX(dv.versionNo) FROM DocumentVersion dv WHERE dv.document.id = :documentId")
    Integer findMaxVersionNoByDocumentId(@Param("documentId") Long documentId);
    
    @Query("SELECT dv FROM DocumentVersion dv WHERE dv.document.id = :documentId ORDER BY dv.versionNo DESC LIMIT 1")
    Optional<DocumentVersion> findLatestVersionByDocumentId(@Param("documentId") Long documentId);
}
