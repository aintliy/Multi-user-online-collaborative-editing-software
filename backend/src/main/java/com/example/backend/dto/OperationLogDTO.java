package com.example.backend.dto;

import com.example.backend.entity.OperationLog;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 操作日志 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationLogDTO {
    private Long id;
    private Long userId;
    private String username;
    private String operationType;  // 对应 entity 的 action
    private String targetType;
    private Long targetId;
    private String description;    // 对应 entity 的 detail
    private LocalDateTime createdAt;
    
    public static OperationLogDTO fromEntity(OperationLog log) {
        return OperationLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : "系统")
                .operationType(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .description(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
