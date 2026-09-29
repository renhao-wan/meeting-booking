package io.github.renhaowan.booking.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.renhaowan.booking.domain.User;
import org.apache.ibatis.annotations.Mapper;

/** 用户表 Mapper。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
