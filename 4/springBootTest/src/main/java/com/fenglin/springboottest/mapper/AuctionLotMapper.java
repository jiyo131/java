package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AuctionLot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuctionLotMapper extends BaseMapper<AuctionLot> {
}
