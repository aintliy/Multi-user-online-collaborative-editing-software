package com.example.backend.repository;

import com.example.backend.entity.DocumentShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 文档分享链接仓库
 */
@Repository
public interface DocumentShareLinkRepository extends JpaRepository<DocumentShareLink, Long> {
    
    /**
     * 根据token查找分享链接
     */
    Optional<DocumentShareLink> findByToken(String token);
    
    /**
     * 查找有效的分享链接（未使用且未过期）
     */
    @Query("SELECT l FROM DocumentShareLink l WHERE l.token = :token AND l.isUsed = false AND l.expiresAt > :now")
    Optional<DocumentShareLink> findValidLink(String token, LocalDateTime now);
    
    /**
     * 检查token是否存在
     */
    boolean existsByToken(String token);
}
