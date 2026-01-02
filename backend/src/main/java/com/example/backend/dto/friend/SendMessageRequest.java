package com.example.backend.dto.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送好友消息请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;
    
    @NotBlank(message = "消息内容不能为空")
    private String content;
    
    /**
     * 消息类型: TEXT-普通文本, SHARE_LINK-分享链接
     */
    private String messageType = "TEXT";
    
    /**
     * 分享链接ID（如果是分享链接消息）
     */
    private Long shareLinkId;
}
