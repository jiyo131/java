package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AuctionDeal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuctionDealMapper extends BaseMapper<AuctionDeal> {
}
