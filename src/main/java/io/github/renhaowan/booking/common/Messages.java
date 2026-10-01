package io.github.renhaowan.booking.common;

/**
 * 统一响应文案常量。
 *
 * <p>所有面向调用方的提示文案集中于此，避免魔法字符串散落各处；
 * 占位符使用 {@code %s}（{@link String#format} 渲染），文案内容必须保持稳定，
 * 因为上游网关与调用方（含大模型）依赖该文案判断失败原因。</p>
 */
public final class Messages {

    /** 参数非法：开始时间必须早于结束时间。 */
    public static final String TIME_ORDER_INVALID = "开始时间必须早于结束时间";

    /** 参数非法：检索窗口必须同时提供起止时间。 */
    public static final String WINDOW_TIME_PAIR_REQUIRED = "startTime 与 endTime 必须同时提供";

    /** 参数校验兜底文案（无具体字段错误时）。 */
    public static final String PARAM_VALIDATION_FAILED = "参数校验失败";

    /** 请求体不可读（典型：时间/日期格式不符合契约）。 */
    public static final String BODY_UNREADABLE = "请求体格式错误：%s";

    /** 认证失败：未携带 Bearer 令牌。 */
    public static final String MISSING_BEARER_TOKEN = "缺少 Bearer 访问令牌";

    /** 认证失败：令牌缺少用户标识。 */
    public static final String TOKEN_MISSING_IDENTITY = "访问令牌缺少用户信息";

    /** 认证失败：令牌无效或已过期。 */
    public static final String TOKEN_INVALID = "访问令牌无效或已过期";

    /** 登录失败：用户名或密码错误（不区分二者，防止用户名枚举）。 */
    public static final String AUTH_FAILED = "用户名或密码错误";

    /** 会议室不存在，占位符为会议室 id。 */
    public static final String ROOM_NOT_FOUND = "会议室不存在: %s";

    /** 时段冲突：该会议室在所选时段已被预订。 */
    public static final String TIME_CONFLICT = "该会议室在所选时段已被预订";

    /** 幂等重放：相同幂等键的预订已存在。 */
    public static final String IDEMPOTENT_REPLAY = "重复请求：相同幂等键的预订已存在";

    /** 预订不存在（含非本人预订的防枚举场景），占位符为预订 id。 */
    public static final String BOOKING_NOT_FOUND = "预订不存在: %s";

    /** 无权操作：仅预订人本人可取消。 */
    public static final String CANCEL_FORBIDDEN = "仅预订人本人可取消预订";

    /** 状态机非法流转：当前状态不允许取消，占位符为当前状态。 */
    public static final String STATUS_NOT_CANCELLABLE = "预订状态为 %s，不允许取消";

    /** 取消并发冲突：乐观锁重试耗尽。 */
    public static final String CANCEL_WRITE_CONFLICT = "取消失败：并发修改冲突，请稍后重试";

    /** 创建并发冲突：乐观锁写入重试耗尽。 */
    public static final String CREATE_WRITE_CONFLICT = "预订写入冲突，请稍后重试";

    /** 未预期异常的对外文案（细节仅记日志，不外泄）。 */
    public static final String INTERNAL_ERROR = "服务内部异常";

    private Messages() {
    }
}
