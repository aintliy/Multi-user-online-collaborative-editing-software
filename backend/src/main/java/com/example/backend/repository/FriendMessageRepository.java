package com.example.backend.repository;

import com.example.backend.entity.FriendMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 好友私聊消息仓库
 */
@Repository
public interface FriendMessageRepository extends JpaRepository<FriendMessage, Long> {
    
    /**
     * 获取两个用户之间的聊天记录
     */
    @Query("SELECT m FROM FriendMessage m WHERE " +
           "(m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1) " +
           "ORDER BY m.createdAt ASC")
    List<FriendMessage> findMessagesBetweenUsers(Long userId1, Long userId2);
    
    /**
     * 分页获取两个用户之间的聊天记录
     */
    @Query("SELECT m FROM FriendMessage m WHERE " +
           "(m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1)")
    Page<FriendMessage> findMessagesBetweenUsers(Long userId1, Long userId2, Pageable pageable);
    
    /**
     * 将某用户发给当前用户的所有消息标记为已读
     */
    @Modifying
    @Query("UPDATE FriendMessage m SET m.isRead = true WHERE m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.isRead = false")
    void markMessagesAsRead(Long senderId, Long receiverId);
    
    /**
     * 获取未读消息数量
     */
    @Query("SELECT COUNT(m) FROM FriendMessage m WHERE m.receiver.id = :userId AND m.isRead = false")
    Long countUnreadMessages(Long userId);
    
    /**
     * 获取与某好友的未读消息数量
     */
    @Query("SELECT COUNT(m) FROM FriendMessage m WHERE m.sender.id = :friendId AND m.receiver.id = :userId AND m.isRead = false")
    Long countUnreadMessagesFromFriend(Long friendId, Long userId);
}
