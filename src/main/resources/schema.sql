-- 会议预约业务单体 DDL（幂等：重复启动不重复建表）
CREATE TABLE IF NOT EXISTS `user`
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL UNIQUE COMMENT '登录名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'SHA-256 十六进制小写',
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN/USER',
    created_at    DATETIME              DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '用户表';

CREATE TABLE IF NOT EXISTS `room`
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(64)  NOT NULL COMMENT '会议室名称',
    capacity      INT          NOT NULL COMMENT '容量（人）',
    has_projector TINYINT      NOT NULL DEFAULT 0 COMMENT '是否有投影：0 否 / 1 是',
    location      VARCHAR(128) NULL COMMENT '位置',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 可用 / 0 停用'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '会议室表';

CREATE TABLE IF NOT EXISTS `booking`
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id        BIGINT       NOT NULL COMMENT '会议室 id',
    user_id        BIGINT       NOT NULL COMMENT '预订人 id',
    title          VARCHAR(128) NOT NULL COMMENT '会议主题',
    start_time     DATETIME     NOT NULL COMMENT '开始时间',
    end_time       DATETIME     NOT NULL COMMENT '结束时间',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 BOOKED / 1 CANCELLED / 2 COMPLETED',
    version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    idempotent_key VARCHAR(64)  NOT NULL COMMENT '幂等键',
    created_at     DATETIME              DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_booking_idempotent_key (idempotent_key),
    INDEX idx_room_time (room_id, start_time, end_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '会议预订表';
