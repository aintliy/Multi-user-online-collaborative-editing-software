package com.example.backend.dto.document;

import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * 返回协作缓存态信息。
 */
@Data
@Builder
public class DocumentCacheResponse {

    private String confirmedContent;
    private String userDraftContent;
    private Set<Long> onlineUsers;
}
