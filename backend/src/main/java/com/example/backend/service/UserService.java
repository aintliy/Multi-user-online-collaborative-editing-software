package com.example.backend.service;

import com.example.backend.dto.auth.UserDTO;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * 根据ID获取用户
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
    }
    
    /**
     * 根据邮箱获取用户
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
    }
    
    /**
     * 根据公开ID获取用户
     */
    public User getUserByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
    }
    
    /**
     * 搜索用户
     */
    public List<UserDTO> searchUsers(String keyword) {
        List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrPublicIdContainingIgnoreCase(keyword, keyword);
        return users.stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
