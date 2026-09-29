package io.github.renhaowan.booking.domain;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 预订状态机：仅允许 BOOKED -&gt; CANCELLED / BOOKED -&gt; COMPLETED。
 */
public enum BookingStatus {

    /** 已预订（初始态）。 */
    BOOKED(0),
    /** 已取消（终态）。 */
    CANCELLED(1),
    /** 已完成（终态）。 */
    COMPLETED(2);

    /** 数据库存储值。 */
    @EnumValue
    private final int code;

    BookingStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 状态机流转校验。
     *
     * @param target 目标状态
     * @return 是否允许从当前状态流转到目标状态
     */
    public boolean canTransitTo(BookingStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case BOOKED -> target == CANCELLED || target == COMPLETED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}
