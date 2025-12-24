package com.example.backend.repository;

import com.example.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);
    
    Page<Notification> findByReceiverIdAndIsReadOrderByCreatedAtDesc(Long receiverId, Boolean isRead, Pageable pageable);
    
    long countByReceiverIdAndIsRead(Long receiverId, Boolean isRead);
}
