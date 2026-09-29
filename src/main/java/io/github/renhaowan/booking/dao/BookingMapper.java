package io.github.renhaowan.booking.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.renhaowan.booking.domain.Booking;
import org.apache.ibatis.annotations.Mapper;

/** 会议预订表 Mapper。 */
@Mapper
public interface BookingMapper extends BaseMapper<Booking> {
}
