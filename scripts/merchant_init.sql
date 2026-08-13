-- 插入商家用户
INSERT INTO user (username, password, nickname, email, phone, role, status)
VALUES ('merchant1', '$2a$10$1vgEVMJGLTE1ZCW2s69dieoMkyFao9Sfd5oQHy4obyXEHeRkvtAxS', '示例商家', 'merchant1@example.com', '13800138002', 'MERCHANT', 1)
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- 插入示例店铺
INSERT INTO store (merchant_id, store_name, store_description, contact_phone, contact_email, address, status)
VALUES (3, '示例数码旗舰店', '专营数码产品，品质保证，售后无忧。', '13800138002', 'merchant1@example.com', '北京市海淀区中关村大街1号', 1)
ON DUPLICATE KEY UPDATE store_name = VALUES(store_name);
