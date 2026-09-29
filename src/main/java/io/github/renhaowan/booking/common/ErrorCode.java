package io.github.renhaowan.booking.common;

/**
 * 业务响应码约定。
 *
 * <p>0 表示成功；1000 段为业务错误码。</p>
 */
public final class ErrorCode {

    /** 成功。 */
    public static final int OK = 0;

    /** 参数非法（HTTP 层语义 400）。 */
    public static final int INVALID_PARAM = 400;

    /** 认证失败（未携带/无效令牌，HTTP 层语义 401）。 */
    public static final int UNAUTHORIZED = 401;

    /** 登录失败：用户名或密码错误。 */
    public static final int AUTH_FAILED = 1000;

    /** 会议室不存在。 */
    public static final int ROOM_NOT_FOUND = 1001;

    /** 时段冲突（含并发写入冲突重试失败）。 */
    public static final int TIME_CONFLICT = 1002;

    /** 状态机非法流转。 */
    public static final int INVALID_STATUS_TRANSITION = 1003;

    /** 幂等键重复（请求重放）。 */
    public static final int IDEMPOTENT_REPLAY = 1004;

    /** 预订不存在。 */
    public static final int BOOKING_NOT_FOUND = 1005;

    /** 无权操作他人预订。 */
    public static final int BOOKING_FORBIDDEN = 1006;

    /** 服务内部异常。 */
    public static final int INTERNAL_ERROR = 500;

    private ErrorCode() {
    }
}
