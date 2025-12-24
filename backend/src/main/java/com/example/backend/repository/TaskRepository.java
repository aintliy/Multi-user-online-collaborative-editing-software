package com.example.backend.repository;

import com.example.backend.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    Page<Task> findByDocumentId(Long documentId, Pageable pageable);
    
    List<Task> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    
    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);
    
    Page<Task> findByCreatorId(Long creatorId, Pageable pageable);
    
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, String status);
}
