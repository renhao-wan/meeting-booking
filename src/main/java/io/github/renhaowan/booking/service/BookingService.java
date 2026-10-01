package io.github.renhaowan.booking.service;

import io.github.renhaowan.booking.domain.Booking;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预订服务：创建（幂等 + 冲突校验 + 乐观锁写入重试）与取消（状态机 + 仅本人 + 乐观锁更新）。
 *
 * <p>并发控制说明：时段重叠校验与写入并非原子，极端并发下可能同时通过校验，
 * 最终由幂等键唯一索引（防重放）与 @Version 乐观锁（防并发写）在数据库层兜底，
 * 服务层以有限次重试消化冲突，重试耗尽后归一为 1002 让调用方稍后重试。</p>
 */
public interface BookingService {

    /**
     * 创建预订，事务内执行：
     * <ol>
     *   <li>① 幂等：同 idempotentKey 既有记录 → 1004 携带既有记录（插入时唯一键冲突亦归一为 1004）</li>
     *   <li>② 房间存在校验 → 1001</li>
     *   <li>③ 时段重叠校验（仅 BOOKED）→ 1002</li>
     *   <li>④ 乐观锁插入（@Version），写入失败重试 1 次后仍失败 → 1002</li>
     * </ol>
     *
     * <p>并发考虑：幂等预检（①）与插入（④）之间存在窗口，两个携带相同幂等键的请求可能同时
     * 通过预检；此时仅其一插入成功，另一方在数据库层触发唯一键冲突，
     * 由 {@code insertWithRetry} 捕获 {@link DuplicateKeyException} 后回读既有记录，
     * 语义上等价于命中①的幂等重放分支，保证对调用方的响应一致。</p>
     *
     * @param userId        预订人 id（来自鉴权上下文）
     * @param roomId        会议室 id
     * @param title         会议主题
     * @param startTime     开始时间（必须早于 endTime）
     * @param endTime       结束时间
     * @param idempotentKey 服务端派生的幂等键，见 BookingController#idempotentKey
     * @return 创建成功的预订（含自增 id 与乐观锁版本号）
     */
    Booking create(Long userId, Long roomId, String title,
                   LocalDateTime startTime, LocalDateTime endTime, String idempotentKey);

    /**
     * 取消预订：仅 BOOKED 可取消（状态机），仅本人可取消，乐观锁更新 status=CANCELLED。
     *
     * <p>并发考虑：读到的记录在更新前可能已被并发修改（如他人取消），@Version 乐观锁会令
     * update 影响 0 行；此时重读最新版本并重新校验"存在 + 本人 + 状态机"三道闸后再试，
     * 既避免把过期状态写回数据库，也保证重试时不会绕过状态机与归属校验。
     * 重试耗尽仍冲突则返回 1002，让调用方以最新状态决定后续动作。</p>
     *
     * @param bookingId 预订 id
     * @param userId    当前用户 id（必须与预订人一致）
     * @return 取消后的预订（status=CANCELLED，version 已递增）
     */
    Booking cancel(Long bookingId, Long userId);

    /** 当前用户的预订列表，按创建时间倒序。 */
    List<Booking> listByUser(Long userId);

    /** 查询本人预订详情；不存在或非本人的记录一律按不存在处理。 */
    Booking getOwned(Long bookingId, Long userId);
}
