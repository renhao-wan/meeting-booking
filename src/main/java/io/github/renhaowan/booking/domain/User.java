package io.github.renhaowan.booking.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Data
@TableName("`user`")
public class User {

    /** 角色：系统管理员。 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 角色：普通用户。 */
    public static final String ROLE_USER = "USER";

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，唯一。 */
    private String username;

    /** SHA-256 十六进制小写口令摘要。 */
    private String passwordHash;

    /** 角色：ADMIN / USER。 */
    private String role;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
