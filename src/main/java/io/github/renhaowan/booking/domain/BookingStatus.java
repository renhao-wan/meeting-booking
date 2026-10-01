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
     * 状态机流转校验：BOOKED 允许流转到 CANCELLED / COMPLETED；CANCELLED 与 COMPLETED 为终态，
     * 不允许再流转。未列出的流转（如重复取消、完成后取消）一律拒绝。
     *
     * <p>并发考虑：该校验基于调用方读到的内存状态，读后状态可能已被并发修改；
     * 取消路径通过 @Version 乐观锁在写入时兜底——版本不符则影响 0 行，
     * 由服务层重读并重新执行本校验，保证终态不会被并发改写。</p>
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
