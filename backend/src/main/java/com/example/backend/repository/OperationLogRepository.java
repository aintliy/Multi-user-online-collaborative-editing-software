package com.example.backend.repository;

import com.example.backend.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    Page<OperationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<OperationLog> findByUserId(Long userId, Pageable pageable);
    
    Page<OperationLog> findByAction(String action, Pageable pageable);
    
    Page<OperationLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId, Pageable pageable);
}
