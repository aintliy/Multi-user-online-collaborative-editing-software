package com.example.backend.dto.friend;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送好友请求DTO
 */
@Data
public class SendFriendRequest {

    @NotNull(message = "目标用户ID不能为空")
    @JsonProperty("userId")
    private Long targetUserId;
    
    private String message;
}
