package com.example.backend.dto.document;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 仅携带提交说明，内容将从 Redis confirmed 缓存读取。
 */
@Data
public class CommitFromCacheRequest {

    @NotBlank(message = "提交说明不能为空")
    private String commitMessage;
}
