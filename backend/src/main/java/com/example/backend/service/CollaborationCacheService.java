package com.example.backend.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 提供协作编辑过程中的 Redis 缓存读写能力。
 * 仅承担“编辑态缓存”职责，不保证持久化可靠性。
 */
@Service
@RequiredArgsConstructor
public class CollaborationCacheService {

    private static final Duration DEFAULT_DRAFT_TTL = Duration.ofSeconds(60);
    private static final Duration DEFAULT_SAVE_LOCK_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate stringRedisTemplate;

    public void saveDraft(Long documentId, Long userId, String content) {
        if (documentId == null || userId == null || content == null) {
            return;
        }
        String key = draftKey(documentId, userId);
        stringRedisTemplate.opsForValue().set(key, content, DEFAULT_DRAFT_TTL);
    }

    public String getDraft(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(draftKey(documentId, userId));
    }

    public void clearDraft(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            return;
        }
        stringRedisTemplate.delete(draftKey(documentId, userId));
    }

    public void saveConfirmed(Long documentId, String content) {
        if (documentId == null || content == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(confirmedKey(documentId), content);
    }

    public String getConfirmed(Long documentId) {
        if (documentId == null) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(confirmedKey(documentId));
    }

    public boolean acquireSaveLock(Long documentId, String token) {
        if (documentId == null || token == null) {
            return false;
        }
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(saveLockKey(documentId), token, DEFAULT_SAVE_LOCK_TTL);
        return Boolean.TRUE.equals(locked);
    }

    public void releaseSaveLock(Long documentId, String token) {
        if (documentId == null || token == null) {
            return;
        }
        String key = saveLockKey(documentId);
        String current = stringRedisTemplate.opsForValue().get(key);
        if (token.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }

    public void addOnlineUser(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            return;
        }
        stringRedisTemplate.opsForSet().add(onlineUsersKey(documentId), userId.toString());
    }

    public void removeOnlineUser(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(onlineUsersKey(documentId), userId.toString());
    }

    public Set<Long> getOnlineUsers(Long documentId) {
        if (documentId == null) {
            return Set.of();
        }
        Set<String> members = stringRedisTemplate.opsForSet().members(onlineUsersKey(documentId));
        if (members == null || members.isEmpty()) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    public void clearDocumentState(Long documentId) {
        if (documentId == null) {
            return;
        }
        stringRedisTemplate.delete(confirmedKey(documentId));
        stringRedisTemplate.delete(saveLockKey(documentId));
        stringRedisTemplate.delete(onlineUsersKey(documentId));
        clearAllDrafts(documentId);
    }

    /**
     * 清理内容相关缓存，但保留在线列表。
     */
    public void clearContentCaches(Long documentId) {
        if (documentId == null) {
            return;
        }
        stringRedisTemplate.delete(confirmedKey(documentId));
        stringRedisTemplate.delete(saveLockKey(documentId));
        clearAllDrafts(documentId);
    }

    public void clearAllDrafts(Long documentId) {
        if (documentId == null) {
            return;
        }
        String pattern = draftKey(documentId, "*");
        Set<String> keys = new HashSet<>();
        var connectionFactory = stringRedisTemplate.getConnectionFactory();
        if (connectionFactory == null) {
            return;
        }
        try (Cursor<byte[]> cursor = connectionFactory.getConnection()
                .scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
            cursor.forEachRemaining(item -> keys.add(new String(item)));
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private String draftKey(Long documentId, Long userId) {
        return "doc:" + documentId + ":draft:" + userId;
    }

    private String draftKey(Long documentId, String wildcard) {
        return "doc:" + documentId + ":draft:" + wildcard;
    }

    private String confirmedKey(Long documentId) {
        return "doc:" + documentId + ":confirmed";
    }

    private String saveLockKey(Long documentId) {
        return "doc:" + documentId + ":save:lock";
    }

    private String onlineUsersKey(Long documentId) {
        return "doc:" + documentId + ":users";
    }
}
