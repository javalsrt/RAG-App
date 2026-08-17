USE znxsgltest;

-- 5位暑假培训教师
-- 先插入 teacher 表
INSERT IGNORE INTO teacher (teacher_no, real_name, gender, title, email, phone, status) VALUES
('S2026001', '李明远', 1, '讲师', 'limingyuan@example.com', '13800000001', 1),
('S2026002', '王思远', 1, '副教授', 'wangsy@example.com', '13800000002', 1),
('S2026003', '赵晓峰', 1, '讲师', 'zhaoxf@example.com', '13800000003', 1),
('S2026004', '陈雨桐', 0, '讲师', 'chenyt@example.com', '13800000004', 1),
('S2026005', '孙文博', 1, '助教', 'sunwb@example.com', '13800000005', 1);

-- 再插入 user 表（密码 123456 的 MD5）
-- MD5('123456') = e10adc3949ba59abbe56e057f20f883e
INSERT IGNORE INTO user (username, password_hash, real_name, role, status, phone, email) VALUES
('limingyuan', 'e10adc3949ba59abbe56e057f20f883e', '李明远', 2, 1, '13800000001', 'limingyuan@example.com'),
('wangsy', 'e10adc3949ba59abbe56e057f20f883e', '王思远', 2, 1, '13800000002', 'wangsy@example.com'),
('zhaoxf', 'e10adc3949ba59abbe56e057f20f883e', '赵晓峰', 2, 1, '13800000003', 'zhaoxf@example.com'),
('chenyt', 'e10adc3949ba59abbe56e057f20f883e', '陈雨桐', 2, 1, '13800000004', 'chenyt@example.com'),
('sunwb', 'e10adc3949ba59abbe56e057f20f883e', '孙文博', 2, 1, '13800000005', 'sunwb@example.com');

SELECT id, real_name, username FROM user WHERE real_name IN ('李明远','王思远','赵晓峰','陈雨桐','孙文博') ORDER BY id;
