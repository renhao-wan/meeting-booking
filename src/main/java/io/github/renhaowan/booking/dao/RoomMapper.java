package io.github.renhaowan.booking.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.renhaowan.booking.domain.Room;
import org.apache.ibatis.annotations.Mapper;

/** 会议室表 Mapper。 */
@Mapper
public interface RoomMapper extends BaseMapper<Room> {
}
