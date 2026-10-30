package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserEmotion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserEmotionMapper extends BaseMapper<UserEmotion> {
}
