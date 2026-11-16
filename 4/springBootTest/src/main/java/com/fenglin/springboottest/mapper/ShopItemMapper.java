package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.ShopItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopItemMapper extends BaseMapper<ShopItem> {
}
