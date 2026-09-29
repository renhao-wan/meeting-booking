package io.github.renhaowan.booking.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 会议预订实体。
 */
@TableName("`booking`")
public class Booking {

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室 id。 */
    @TableField("room_id")
    private Long roomId;

    /** 预订人 id。 */
    @TableField("user_id")
    private Long userId;

    /** 会议主题。 */
    private String title;

    /** 开始时间。 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 结束时间。 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 预订状态。 */
    private BookingStatus status;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;

    /** 幂等键，唯一。 */
    @TableField("idempotent_key")
    private String idempotentKey;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public void setIdempotentKey(String idempotentKey) {
        this.idempotentKey = idempotentKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
