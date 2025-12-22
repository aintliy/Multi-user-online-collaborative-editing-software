package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.DocumentInviteLink;

/**
 * 文档邀请链接Mapper接口
 */
@Mapper
public interface DocumentInviteLinkMapper extends BaseMapper<DocumentInviteLink> {
}
