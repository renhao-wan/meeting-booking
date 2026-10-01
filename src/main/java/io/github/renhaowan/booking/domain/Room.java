package io.github.renhaowan.booking.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会议室实体。
 */
@Data
@TableName("`room`")
public class Room {

    /** 会议室状态：可用。 */
    public static final int STATUS_AVAILABLE = 1;

    /** 会议室状态：停用。 */
    public static final int STATUS_DISABLED = 0;

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
}
