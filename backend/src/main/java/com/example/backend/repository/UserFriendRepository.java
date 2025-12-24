package com.example.backend.repository;

import com.example.backend.entity.UserFriend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFriendRepository extends JpaRepository<UserFriend, Long> {
    
    Optional<UserFriend> findByUserIdAndFriendId(Long userId, Long friendId);
    
    List<UserFriend> findByUserIdAndStatus(Long userId, String status);
    
    List<UserFriend> findByFriendIdAndStatus(Long friendId, String status);
    
    @Query("SELECT uf FROM UserFriend uf WHERE (uf.user.id = :userId OR uf.friend.id = :userId) AND uf.status = 'ACCEPTED'")
    List<UserFriend> findAcceptedFriendsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT uf FROM UserFriend uf WHERE uf.friend.id = :userId AND uf.status = 'PENDING'")
    List<UserFriend> findPendingRequestsForUser(@Param("userId") Long userId);
    
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
    
    @Query("SELECT CASE WHEN COUNT(uf) > 0 THEN true ELSE false END FROM UserFriend uf " +
           "WHERE ((uf.user.id = :userId1 AND uf.friend.id = :userId2) OR " +
           "(uf.user.id = :userId2 AND uf.friend.id = :userId1)) AND uf.status = 'ACCEPTED'")
    boolean areFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
