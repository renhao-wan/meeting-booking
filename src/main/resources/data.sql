-- 会议预约业务单体种子数据（幂等：NOT EXISTS 保证重复启动不重复插入）
-- admin 口令为 admin123，password_hash = SHA-256("admin123") 十六进制小写
INSERT INTO `user` (username, password_hash, role, created_at)
SELECT 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'admin');

INSERT INTO `room` (name, capacity, has_projector, location, status)
SELECT 'B201', 4, 0, 'B栋2层', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `room` WHERE name = 'B201');

INSERT INTO `room` (name, capacity, has_projector, location, status)
SELECT 'B305', 8, 1, 'B栋3层', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `room` WHERE name = 'B305');

INSERT INTO `room` (name, capacity, has_projector, location, status)
SELECT 'B401', 20, 1, 'B栋4层', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `room` WHERE name = 'B401');
