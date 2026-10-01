package io.github.renhaowan.booking.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.dao.BookingMapper;
import io.github.renhaowan.booking.dao.RoomMapper;
import io.github.renhaowan.booking.domain.Booking;
import io.github.renhaowan.booking.domain.BookingStatus;
import io.github.renhaowan.booking.domain.Room;
import io.github.renhaowan.booking.service.impl.RoomServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 会议室检索单元测试：重叠排除逻辑（mock mapper 返回既有预订）。
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);

    private static final LocalTime START_TIME = LocalTime.of(9, 0);

    private static final LocalTime END_TIME = LocalTime.of(11, 0);

    private static final LocalDateTime WINDOW_START = DATE.atTime(START_TIME);

    private static final LocalDateTime WINDOW_END = DATE.atTime(END_TIME);

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private BookingMapper bookingMapper;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 解析 Room::getXxx / Booking::getXxx 依赖 MP 表信息缓存，单测环境手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Room.class);
        TableInfoHelper.initTableInfo(assistant, Booking.class);
        roomService = new RoomServiceImpl(roomMapper, bookingMapper);
    }

    private Room room(long id, String name, int capacity, boolean projector) {
        Room room = new Room();
        room.setId(id);
        room.setName(name);
        room.setCapacity(capacity);
        room.setHasProjector(projector);
        room.setStatus(Room.STATUS_AVAILABLE);
        return room;
    }

    private Booking booked(long id, long roomId) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setRoomId(roomId);
        booking.setUserId(2L);
        booking.setTitle("周会");
        booking.setStartTime(WINDOW_START);
        booking.setEndTime(WINDOW_END);
        booking.setStatus(BookingStatus.BOOKED);
        booking.setVersion(0);
        booking.setIdempotentKey("key-" + id);
        return booking;
    }

    @Test
    void searchExcludesRoomsWithOverlappingBookings() {
        Room b201 = room(1L, "B201", 4, false);
        Room b305 = room(2L, "B305", 8, true);
        when(roomMapper.selectList(any())).thenReturn(List.of(b201, b305));
        // 既有预订：B201 与 [9:00, 11:00) 重叠
        when(bookingMapper.selectList(any())).thenReturn(List.of(booked(9L, 1L)));

        List<Room> result = roomService.searchAvailable(DATE, START_TIME, END_TIME, null, null);

        assertEquals(List.of(b305), result);

        // 校验重叠条件 SQL：existing.start < :end AND existing.end > :start，且仅统计 BOOKED
        // Mockito captor 的类型必须与实际参数类型精确匹配（LambdaQueryWrapper 与 QueryWrapper 互为兄弟类）
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<LambdaQueryWrapper<Booking>> captor =
                (ArgumentCaptor) ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(bookingMapper).selectList(captor.capture());
        LambdaQueryWrapper<Booking> wrapper = captor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("start_time <"), sqlSegment);
        assertTrue(sqlSegment.contains("end_time >"), sqlSegment);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(WINDOW_START));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(WINDOW_END));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(BookingStatus.BOOKED));
    }

    @Test
    void searchWithoutTimeWindowReturnsAllAvailable() {
        Room b201 = room(1L, "B201", 4, false);
        Room b305 = room(2L, "B305", 8, true);
        when(roomMapper.selectList(any())).thenReturn(List.of(b201, b305));

        List<Room> result = roomService.searchAvailable(null, null, null, null, null);

        assertEquals(List.of(b201, b305), result);
        verifyNoInteractions(bookingMapper);
    }

    @Test
    void searchFiltersByCapacityAndProjector() {
        Room b401 = room(3L, "B401", 20, true);
        when(roomMapper.selectList(any())).thenReturn(List.of(b401));
        when(bookingMapper.selectList(any())).thenReturn(List.of());

        List<Room> result = roomService.searchAvailable(DATE, START_TIME, END_TIME, 8, true);

        assertEquals(List.of(b401), result);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<LambdaQueryWrapper<Room>> captor =
                (ArgumentCaptor) ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(roomMapper).selectList(captor.capture());
        LambdaQueryWrapper<Room> wrapper = captor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("capacity >="), sqlSegment);
        assertTrue(sqlSegment.contains("has_projector ="), sqlSegment);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(8));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Boolean.TRUE));
    }

    @Test
    void searchWithPartialTimeWindowThrows400() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> roomService.searchAvailable(DATE, START_TIME, null, null, null));

        assertEquals(400, e.getCode());
    }

    @Test
    void searchWithInvertedTimeWindowThrows400() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> roomService.searchAvailable(DATE, END_TIME, START_TIME, null, null));

        assertEquals(400, e.getCode());
    }
}
