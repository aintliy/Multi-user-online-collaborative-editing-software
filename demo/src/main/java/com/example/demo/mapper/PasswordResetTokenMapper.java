package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.PasswordResetToken;

/**
 * 密码重置令牌Mapper
 */
@Mapper
public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {
}
