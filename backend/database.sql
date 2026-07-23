CREATE DATABASE IF NOT EXISTS ecommerce
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce;

CREATE TABLE IF NOT EXISTS user (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    username VARCHAR(50) NOT NULL UNIQUE,
                                    password VARCHAR(100) NOT NULL,
                                    nickname VARCHAR(50),
                                    email VARCHAR(100),
                                    phone VARCHAR(20),
                                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                                    status INT DEFAULT 1,
                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @user_role_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce'
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'role'
);
SET @user_role_sql := IF(
        @user_role_exists = 0,
        'ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT ''USER'' AFTER `phone`',
        'SELECT 1'
                      );
PREPARE stmt_user_role FROM @user_role_sql;
EXECUTE stmt_user_role;
DEALLOCATE PREPARE stmt_user_role;

CREATE TABLE IF NOT EXISTS product (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       name VARCHAR(100) NOT NULL,
                                       description TEXT,
                                       price DECIMAL(10, 2) NOT NULL,
                                       stock INT DEFAULT 0,
                                       image VARCHAR(255),
                                       category_id INT,
                                       merchant_id BIGINT NULL,
                                       status INT DEFAULT 1,
                                       create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `order` (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       user_id BIGINT NOT NULL,
                                       order_no VARCHAR(32) NOT NULL UNIQUE,
                                       total_amount DECIMAL(10, 2) NOT NULL,
                                       status INT DEFAULT 0,
                                       address VARCHAR(255),
                                       phone VARCHAR(20),
                                       receiver VARCHAR(50),
                                       create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS order_item (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          order_id BIGINT NOT NULL,
                                          product_id BIGINT NOT NULL,
                                          product_name VARCHAR(100) NOT NULL,
                                          price DECIMAL(10, 2) NOT NULL,
                                          quantity INT NOT NULL,
                                          CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES `order`(id),
                                          CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS cart_item (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         user_id BIGINT NOT NULL,
                                         product_id BIGINT NOT NULL,
                                         quantity INT NOT NULL,
                                         create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                         update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         CONSTRAINT fk_cart_item_user FOREIGN KEY (user_id) REFERENCES user(id),
                                         CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES product(id),
                                         CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE IF NOT EXISTS user_address (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            user_id BIGINT NOT NULL,
                                            receiver VARCHAR(50) NOT NULL,
                                            phone VARCHAR(20) NOT NULL,
                                            province VARCHAR(50) NOT NULL,
                                            city VARCHAR(50) NOT NULL,
                                            district VARCHAR(50) NOT NULL,
                                            detail_address VARCHAR(255) NOT NULL,
                                            is_default TINYINT(1) DEFAULT 0,
                                            create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS payment_callback_log (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                    order_no VARCHAR(32),
                                                    trade_no VARCHAR(64),
                                                    trade_status VARCHAR(32),
                                                    raw_payload TEXT NOT NULL,
                                                    verified TINYINT(1) DEFAULT 0,
                                                    processed TINYINT(1) DEFAULT 0,
                                                    success TINYINT(1) DEFAULT 0,
                                                    error_message VARCHAR(255),
                                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                    KEY idx_payment_callback_order_no (order_no),
                                                    KEY idx_payment_callback_trade_no (trade_no)
);

CREATE TABLE IF NOT EXISTS showcase_strategy_config (
                                                        id BIGINT PRIMARY KEY,
                                                        mode VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
                                                        short_window_days INT NOT NULL DEFAULT 7,
                                                        long_window_days INT NOT NULL DEFAULT 30,
                                                        cart_preference_weight DECIMAL(6, 4) NOT NULL DEFAULT 0.6000,
                                                        hot_weights_json TEXT NOT NULL,
                                                        anonymous_weights_json TEXT NOT NULL,
                                                        personalized_weights_json TEXT NOT NULL,
                                                        hot_signal_weights_json TEXT NOT NULL,
                                                        last_auto_tuned_at DATETIME NULL,
                                                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                        update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_view_event (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                  product_id BIGINT NOT NULL,
                                                  user_id BIGINT NULL,
                                                  source VARCHAR(32) NOT NULL,
                                                  view_date DATE NOT NULL,
                                                  viewed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                  KEY idx_product_view_event_product_date (product_id, view_date),
                                                  KEY idx_product_view_event_user_date (user_id, view_date)
);

CREATE TABLE IF NOT EXISTS product_metric_daily (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                    metric_date DATE NOT NULL,
                                                    product_id BIGINT NOT NULL,
                                                    view_count INT NOT NULL DEFAULT 0,
                                                    cart_add_count INT NOT NULL DEFAULT 0,
                                                    paid_order_count INT NOT NULL DEFAULT 0,
                                                    paid_quantity INT NOT NULL DEFAULT 0,
                                                    paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                    UNIQUE KEY uk_product_metric_daily_date_product (metric_date, product_id),
                                                    KEY idx_product_metric_daily_date (metric_date)
);

CREATE TABLE IF NOT EXISTS store (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     merchant_id BIGINT NOT NULL,
                                     store_name VARCHAR(100) NOT NULL,
                                     store_description TEXT,
                                     store_logo VARCHAR(255),
                                     contact_phone VARCHAR(20),
                                     contact_email VARCHAR(100),
                                     address VARCHAR(255),
                                     status INT DEFAULT 1,
                                     create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     CONSTRAINT fk_store_merchant FOREIGN KEY (merchant_id) REFERENCES user(id),
                                     UNIQUE KEY uk_store_merchant (merchant_id)
);

CREATE TABLE IF NOT EXISTS review (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      product_id BIGINT NOT NULL,
                                      user_id BIGINT NOT NULL,
                                      order_id BIGINT NULL,
                                      rating INT NOT NULL DEFAULT 5,
                                      content TEXT,
                                      reply TEXT,
                                      reply_time DATETIME NULL,
                                      status INT DEFAULT 1,
                                      create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                      update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES product(id),
                                      CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES user(id),
                                      KEY idx_review_product (product_id),
                                      KEY idx_review_user (user_id)
);

INSERT INTO showcase_strategy_config (
    id,
    mode,
    short_window_days,
    long_window_days,
    cart_preference_weight,
    hot_weights_json,
    anonymous_weights_json,
    personalized_weights_json,
    hot_signal_weights_json,
    last_auto_tuned_at
)
VALUES
    (
        1,
        'MANUAL',
        7,
        30,
        0.6000,
        '{"sales":0.55,"revenue":0.15,"orders":0.15,"freshness":0.10,"inventory":0.05}',
        '{"hot":0.50,"freshness":0.25,"inventory":0.15,"affordability":0.10}',
        '{"category":0.50,"hot":0.25,"price":0.10,"freshness":0.10,"inventory":0.05}',
        '{"sales":0.50,"revenue":0.20,"orders":0.20,"freshness":0.10}',
        NULL
    )
ON DUPLICATE KEY UPDATE
                     mode = VALUES(mode),
                     short_window_days = VALUES(short_window_days),
                     long_window_days = VALUES(long_window_days),
                     cart_preference_weight = VALUES(cart_preference_weight),
                     hot_weights_json = VALUES(hot_weights_json),
                     anonymous_weights_json = VALUES(anonymous_weights_json),
                     personalized_weights_json = VALUES(personalized_weights_json),
                     hot_signal_weights_json = VALUES(hot_signal_weights_json);

INSERT INTO user (username, password, nickname, email, phone, role, status)
VALUES
    ('admin', '$2a$10$4A/NkddspelKpwdq9zRPF.ZRCFzEXtDvxARmaOEzsPvPdDTqRtNXS', '管理员', 'admin@example.com', '13800138000', 'ADMIN', 1),
    ('user1', '$2a$10$1vgEVMJGLTE1ZCW2s69dieoMkyFao9Sfd5oQHy4obyXEHeRkvtAxS', '用户1', 'user1@example.com', '13800138001', 'USER', 1),
    ('merchant1', '$2a$10$1vgEVMJGLTE1ZCW2s69dieoMkyFao9Sfd5oQHy4obyXEHeRkvtAxS', '示例商家', 'merchant1@example.com', '13800138002', 'MERCHANT', 1)
ON DUPLICATE KEY UPDATE
                     password = VALUES(password),
                     nickname = VALUES(nickname),
                     email = VALUES(email),
                     phone = VALUES(phone),
                     role = VALUES(role),
                     status = VALUES(status);

-- 商品图片使用 frontend/public/images/products/free/ 下的免费本地资源，避免依赖外链或占位图。
INSERT INTO product (id, name, description, price, stock, image, category_id, merchant_id, status)
VALUES
    (1, '旗舰手机 Pro', '高性能智能手机，支持高刷屏与快速充电。', 3999.00, 100, '/images/products/free/digital-smartphone-alt.jpg', 1, 3, 1),
    (2, '蓝牙降噪耳机', '支持主动降噪和长续航，通勤办公两相宜。', 699.00, 80, '/images/products/free/audio-speaker-detail.jpg', 6, 3, 1),
    (3, '机械键盘', '办公与游戏都适用的机械键盘，支持热插拔。', 399.00, 60, '/images/products/free/office-laptop-alt.jpg', 2, 3, 1),
    (4, '27 寸显示器', '2K 高清显示器，适合办公与创作。', 1299.00, 35, '/images/products/free/office-laptop-alt.jpg', 2, 3, 1),
    (5, '轻薄笔记本电脑', '轻薄便携，适合移动办公和学习。', 5299.00, 18, '/images/products/free/office-laptop-alt.jpg', 2, 3, 1),
    (6, '智能空气炸锅', '一键烘烤，低油健康烹饪。', 459.00, 40, '/images/products/free/appliance-airfryer.jpg', 3, 3, 1),
    (7, '无线吸尘器', '大吸力家用吸尘器，续航持久。', 899.00, 26, '/images/products/free/appliance-airfryer.jpg', 3, 3, 1),
    (8, '人体工学办公椅', '久坐舒适，支持腰部承托与多档调节。', 1099.00, 22, '/images/products/free/home-living-room.jpg', 4, 3, 1),
    (9, '折叠晾衣架', '轻巧稳固，阳台收纳更省空间。', 159.00, 75, '/images/products/free/home-living-room.jpg', 4, 3, 1),
    (10, '智能手表', '支持运动记录、消息提醒与心率监测。', 999.00, 48, '/images/products/free/digital-smartphone-alt.jpg', 1, 3, 1),
    (11, '平板电脑', '适合学习、追剧和轻办公。', 2499.00, 31, '/images/products/free/digital-smartphone-alt.jpg', 1, 3, 1),
    (12, '跑步机', '家用可折叠跑步机，支持多种训练模式。', 2999.00, 12, '/images/products/free/sports-gym.jpg', 5, 3, 1),
    (13, '瑜伽垫', '防滑减震，适合家庭训练。', 89.00, 120, '/images/products/free/sports-gym.jpg', 5, 3, 1),
    (14, '动感单车', '磁控静音设计，居家健身更高效。', 1799.00, 14, '/images/products/free/sports-gym.jpg', 5, 3, 1),
    (15, '智能音箱', '支持语音助手、音乐播放与家居联动。', 299.00, 52, '/images/products/free/audio-speaker-detail.jpg', 6, 3, 1),
    (16, '家庭影院音响', '环绕声场设计，观影更沉浸。', 1899.00, 16, '/images/products/free/audio-speaker-detail.jpg', 6, 3, 1),
    (17, '加湿器', '静音补水，营造舒适室内环境。', 199.00, 47, '/images/products/free/appliance-airfryer.jpg', 3, 3, 1),
    (18, '储物收纳柜', '多层分区设计，适合家居整理。', 329.00, 28, '/images/products/free/home-living-room.jpg', 4, 3, 1)
ON DUPLICATE KEY UPDATE
                     name = VALUES(name),
                     description = VALUES(description),
                     price = VALUES(price),
                     stock = VALUES(stock),
                     image = VALUES(image),
                     category_id = VALUES(category_id),
                     merchant_id = VALUES(merchant_id),
                     status = VALUES(status);

INSERT INTO store (merchant_id, store_name, store_description, contact_phone, contact_email, address, status)
VALUES
    (3, '示例数码旗舰店', '专营数码产品，品质保证，售后无忧。', '13800138002', 'merchant1@example.com', '北京市海淀区中关村大街1号', 1)
ON DUPLICATE KEY UPDATE
                     store_name = VALUES(store_name),
                     store_description = VALUES(store_description),
                     contact_phone = VALUES(contact_phone),
                     contact_email = VALUES(contact_email),
                     address = VALUES(address),
                     status = VALUES(status);

INSERT INTO user_address (id, user_id, receiver, phone, province, city, district, detail_address, is_default)
VALUES
    (1, 2, '用户1', '13800138001', '上海市', '上海市', '浦东新区', '世纪大道 100 号', 1),
    (2, 2, '用户1', '13800138001', '北京市', '北京市', '朝阳区', '望京 SOHO T3', 0)
ON DUPLICATE KEY UPDATE
                     receiver = VALUES(receiver),
                     phone = VALUES(phone),
                     province = VALUES(province),
                     city = VALUES(city),
                     district = VALUES(district),
                     detail_address = VALUES(detail_address),
                     is_default = VALUES(is_default);

INSERT INTO `order` (id, user_id, order_no, total_amount, status, address, phone, receiver)
VALUES
    (1, 2, '20260416001', 300.00, 1, '北京市朝阳区', '13800138001', '用户1'),
    (2, 2, '20260416002', 500.00, 0, '上海市浦东新区', '13800138001', '用户1')
ON DUPLICATE KEY UPDATE
                     total_amount = VALUES(total_amount),
                     status = VALUES(status),
                     address = VALUES(address),
                     phone = VALUES(phone),
                     receiver = VALUES(receiver);

INSERT INTO order_item (id, order_id, product_id, product_name, price, quantity)
VALUES
    (1, 1, 1, '手机', 100.00, 1),
    (2, 1, 2, '蓝牙耳机', 200.00, 1),
    (3, 2, 5, '笔记本电脑', 500.00, 1)
ON DUPLICATE KEY UPDATE
                     order_id = VALUES(order_id),
                     product_id = VALUES(product_id),
                     product_name = VALUES(product_name),
                     price = VALUES(price),
                     quantity = VALUES(quantity);

INSERT INTO cart_item (id, user_id, product_id, quantity)
VALUES
    (1, 2, 3, 1),
    (2, 2, 4, 1)
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity);
