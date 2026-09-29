package io.github.renhaowan.booking.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 会议室实体。
 */
@TableName("`room`")
public class Room {

    /** 会议室状态：可用。 */
    public static final int STATUS_AVAILABLE = 1;

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室名称。 */
    private String name;

    /** 容量（人）。 */
    private Integer capacity;

    /** 是否有投影仪。 */
    @TableField("has_projector")
    private Boolean hasProjector;

    /** 位置。 */
    private String location;

    /** 状态：1 可用 / 0 停用。 */
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Boolean getHasProjector() {
        return hasProjector;
    }

    public void setHasProjector(Boolean hasProjector) {
        this.hasProjector = hasProjector;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
