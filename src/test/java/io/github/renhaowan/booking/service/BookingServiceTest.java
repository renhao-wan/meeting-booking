package io.github.renhaowan.booking.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.dao.BookingMapper;
import io.github.renhaowan.booking.dao.RoomMapper;
import io.github.renhaowan.booking.domain.Booking;
import io.github.renhaowan.booking.domain.BookingStatus;
import io.github.renhaowan.booking.domain.Room;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预订服务单元测试：时段冲突、乐观锁重试、幂等重放、状态机（mock mapper，不连数据库）。
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 1, 9, 0);

    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 1, 11, 0);

    private static final String IDEMPOTENT_KEY = "key-0001";

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private RoomMapper roomMapper;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 解析 Booking::getXxx 依赖 MP 表信息缓存，单测环境手动初始化
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Booking.class);
        bookingService = new BookingService(bookingMapper, roomMapper);
    }

    private Room room() {
        Room room = new Room();
        room.setId(1L);
        room.setName("B305");
        room.setCapacity(8);
        room.setHasProjector(true);
        room.setLocation("B栋3层");
        room.setStatus(Room.STATUS_AVAILABLE);
        return room;
    }

    private Booking booking(Long id, Long ownerId, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setRoomId(1L);
        booking.setUserId(ownerId);
        booking.setTitle("周会");
        booking.setStartTime(START);
        booking.setEndTime(END);
        booking.setStatus(status);
        booking.setVersion(0);
        booking.setIdempotentKey(IDEMPOTENT_KEY);
        return booking;
    }

    private void stubCreateHappyPath() {
        when(bookingMapper.selectOne(any())).thenReturn(null);
        when(roomMapper.selectById(1L)).thenReturn(room());
        when(bookingMapper.selectCount(any())).thenReturn(0L);
    }

    // ---------------- 创建 ----------------

    @Test
    void createSuccess() {
        stubCreateHappyPath();
        when(bookingMapper.insert(any(Booking.class))).thenReturn(1);

        Booking result = bookingService.create(2L, 1L, "周会", START, END, IDEMPOTENT_KEY);

        assertNotNull(result);
        assertEquals(BookingStatus.BOOKED, result.getStatus());
        assertEquals(0, result.getVersion());
        verify(bookingMapper, times(1)).insert(any(Booking.class));
    }

    @Test
    void createTimeConflictThrows1002() {
        stubCreateHappyPath();
        when(bookingMapper.selectCount(any())).thenReturn(1L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(2L, 1L, "周会", START, END, IDEMPOTENT_KEY));

        assertEquals(1002, e.getCode());
        verify(bookingMapper, never()).insert(any(Booking.class));
    }

    @Test
    void createRoomNotFoundThrows1001() {
        when(bookingMapper.selectOne(any())).thenReturn(null);
        when(roomMapper.selectById(1L)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(2L, 1L, "周会", START, END, IDEMPOTENT_KEY));

        assertEquals(1001, e.getCode());
    }

    @Test
    void createIdempotentReplayThrows1004WithExisting() {
        Booking existing = booking(9L, 2L, BookingStatus.BOOKED);
        when(bookingMapper.selectOne(any())).thenReturn(existing);

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(2L, 1L, "周会", START, END, IDEMPOTENT_KEY));

        assertEquals(1004, e.getCode());
        assertSame(existing, e.getData());
        verify(bookingMapper, never()).insert(any(Booking.class));
    }

    @Test
    void createConcurrentDuplicateKeyThrows1004WithExisting() {
        Booking existing = booking(9L, 2L, BookingStatus.BOOKED);
        // 第一次 selectOne：幂等预检无记录；插入触发唯一键冲突后第二次 selectOne：命中既有记录
        when(bookingMapper.selectOne(any())).thenReturn(null, existing);
        when(roomMapper.selectById(1L)).thenReturn(room());
        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(bookingMapper.insert(any(Booking.class))).thenThrow(new DuplicateKeyException("uk_booking_idempotent_key"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(2L, 1L, "周会", START, END, IDEMPOTENT_KEY));

        assertEquals(1004, e.getCode());
        assertSame(existing, e.getData());
        verify(bookingMapper, times(1)).insert(any(Booking.class));
    }

    @Test
    void createWriteRetryExhaustedThrows1002() {
        stubCreateHappyPath();
        // 首次写入失败 + 重试 1 次仍失败 → 1002，共尝试 2 次
        when(bookingMapper.insert(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("deadlock"))
                .thenThrow(new DataIntegrityViolationException("deadlock"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(2L, 1L, "周会", START, END, IDEMPOTENT_KEY));

        assertEquals(1002, e.getCode());
        verify(bookingMapper, times(2)).insert(any(Booking.class));
    }

    @Test
    void createInvalidTimeRangeThrows400() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(2L, 1L, "周会", END, START, IDEMPOTENT_KEY));

        assertEquals(400, e.getCode());
    }

    // ---------------- 取消 ----------------

    @Test
    void cancelSuccess() {
        Booking booking = booking(9L, 2L, BookingStatus.BOOKED);
        when(bookingMapper.selectById(9L)).thenReturn(booking);
        when(bookingMapper.updateById(any(Booking.class))).thenReturn(1);

        Booking result = bookingService.cancel(9L, 2L);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(bookingMapper, times(1)).updateById(any(Booking.class));
    }

    @Test
    void cancelInvalidTransitionThrows1003() {
        when(bookingMapper.selectById(9L)).thenReturn(booking(9L, 2L, BookingStatus.CANCELLED));

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.cancel(9L, 2L));

        assertEquals(1003, e.getCode());
    }

    @Test
    void cancelNotOwnerThrows1006() {
        when(bookingMapper.selectById(9L)).thenReturn(booking(9L, 99L, BookingStatus.BOOKED));

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.cancel(9L, 2L));

        assertEquals(1006, e.getCode());
    }

    @Test
    void cancelNotFoundThrows1005() {
        when(bookingMapper.selectById(9L)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.cancel(9L, 2L));

        assertEquals(1005, e.getCode());
    }

    @Test
    void cancelOptimisticRetryExhaustedThrows1002() {
        // 乐观锁冲突：两次更新均 0 行；重读须返回新的实体实例（真实场景为数据库最新行）
        Booking firstRead = booking(9L, 2L, BookingStatus.BOOKED);
        Booking reloaded = booking(9L, 2L, BookingStatus.BOOKED);
        when(bookingMapper.selectById(9L)).thenReturn(firstRead, reloaded);
        when(bookingMapper.updateById(any(Booking.class))).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.cancel(9L, 2L));

        assertEquals(1002, e.getCode());
        verify(bookingMapper, times(2)).updateById(any(Booking.class));
    }
}
