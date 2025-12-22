package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.UserFriend;

/**
 * 好友关系Mapper接口
 */
@Mapper
public interface UserFriendMapper extends BaseMapper<UserFriend> {
}
