package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
}
