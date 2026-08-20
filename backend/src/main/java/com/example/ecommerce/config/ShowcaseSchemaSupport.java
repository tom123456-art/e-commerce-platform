package com.example.ecommerce.config;

import com.example.ecommerce.entity.ShowcaseStrategyConfig;
import com.example.ecommerce.mapper.ShowcaseStrategyConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

// =============================================================================
// 【教学】展示策略数据库 Schema 支持类
// =============================================================================
//
// 一、这个类的作用
// ---------------------------------
// 在应用启动时自动创建展示策略相关的数据库表，并确保默认配置存在。
// 这是一种"数据库 Schema 自动初始化"的模式。
//
// 二、为什么需要这个类？
// ---------------------------------
// 传统的做法是通过 SQL 脚本（如 database.sql）手动创建表结构。
// 但这种方式有几个问题：
//   1. 需要手动执行 SQL 脚本，容易遗忘
//   2. 多环境部署时需要确保每个环境都执行了脚本
//   3. 表结构变更时需要手动迁移
//
// 本类的做法是：在代码中自动检查并创建表（如果不存在）。
// 优点是"自包含"——代码自带建表逻辑，不需要额外的 SQL 脚本。
// 缺点是不适合复杂的 Schema 变更（生产环境推荐使用 Flyway/Liquibase）。
//
// 三、AtomicBoolean（面试常考）
// ---------------------------------
// AtomicBoolean 是 Java 并发包中的原子布尔类型，提供线程安全的布尔操作。
// 这里用于实现"单次初始化"模式——ensureSchema() 方法只执行一次。
//
// 为什么需要双重检查锁定（Double-Checked Locking）？
//   1. 第一次检查（无锁）：快速判断是否已初始化，避免不必要的同步
//   2. synchronized 块：确保只有一个线程执行初始化
//   3. 第二次检查（有锁）：防止在等待锁期间被其他线程抢先初始化
//
// 这是经典的并发设计模式，在单例模式中也经常使用。
//
// 四、JdbcTemplate 是什么？
// ---------------------------------
// JdbcTemplate 是 Spring 提供的 JDBC 操作模板类，封装了：
//   - 连接管理（获取连接、释放连接）
//   - 异常处理（将 SQLException 转为 DataAccessException）
//   - 资源清理（关闭 Statement、ResultSet）
//   - 常见操作（查询、更新、批量操作）
//
// 比原生 JDBC 更简洁、更安全。
//
// 五、为什么同时使用 JdbcTemplate 和 MyBatis Mapper？
// ---------------------------------
// - JdbcTemplate：用于执行 DDL 语句（CREATE TABLE），MyBatis 不适合执行 DDL
// - MyBatis Mapper：用于 DML 操作（SELECT、INSERT、UPDATE），有类型安全的接口
// 两者互补，各取所长。
// =============================================================================

@Component
public class ShowcaseSchemaSupport {

    private static final Logger log = LoggerFactory.getLogger(ShowcaseSchemaSupport.class);

    private final JdbcTemplate jdbcTemplate;
    private final ShowcaseStrategyConfigMapper showcaseStrategyConfigMapper;
    private final ShowcaseProperties showcaseProperties;
    private final ObjectMapper objectMapper;

    /**
     * 初始化状态标志。
     * <p>
     * 【教学】AtomicBoolean 用于线程安全的"是否已初始化"判断。
     * volatile 语义保证多线程间的可见性——一个线程设置为 true 后，
     * 其他线程立即可见，不会读到旧值（false）。
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 构造器注入所有依赖。
     *
     * @param jdbcTemplate                 Spring JDBC 模板（用于执行 DDL）
     * @param showcaseStrategyConfigMapper MyBatis Mapper（用于 DML 操作）
     * @param showcaseProperties           展示策略配置（提供默认值）
     * @param objectMapper                 JSON 序列化工具（用于将权重配置序列化为 JSON）
     */
    public ShowcaseSchemaSupport(JdbcTemplate jdbcTemplate,
                                 ShowcaseStrategyConfigMapper showcaseStrategyConfigMapper,
                                 ShowcaseProperties showcaseProperties,
                                 ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.showcaseStrategyConfigMapper = showcaseStrategyConfigMapper;
        this.showcaseProperties = showcaseProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 【教学】确保数据库 Schema 就绪
     * <p>
     * 使用"双重检查锁定"模式确保只执行一次初始化：
     * 1. 第一次检查（无锁）：如果已初始化，直接返回
     * 2. 获取锁（synchronized）
     * 3. 第二次检查（有锁）：防止并发场景下的重复初始化
     * 4. 执行初始化：建表 + 插入默认配置
     * 5. 标记为已初始化
     * <p>
     * 这个方法不会在应用启动时自动调用，而是由需要使用展示策略的服务在首次使用时调用。
     * 这是一种"懒初始化"（Lazy Initialization）策略。
     */
    public void ensureSchema() {
        // 第一次检查（无锁，快速路径）
        if (initialized.get()) {
            return;
        }
        // 获取锁
        synchronized (this) {
            // 第二次检查（有锁，防止重复初始化）
            if (initialized.get()) {
                return;
            }
            createTables();
            ensureDefaultConfig();
            initialized.set(true);
            log.info("Showcase strategy schema is ready");
        }
    }

    /**
     * 【教学】创建数据库表
     * <p>
     * 使用 CREATE TABLE IF NOT EXISTS 语句，如果表已存在则不会报错。
     * 这种"幂等"操作可以安全地多次执行。
     * <p>
     * 创建了三张表：
     * 1. showcase_strategy_config：展示策略配置表
     * 2. product_view_event：商品浏览事件表
     * 3. product_metric_daily：商品每日指标表
     */
    private void createTables() {
        // 【教学】展示策略配置表
        // 存储展示策略的权重配置和运行模式（手动/自动调优）
        jdbcTemplate.execute("create table if not exists showcase_strategy_config ("
                + "id bigint primary key,"
                + "mode varchar(16) not null,"                    // MANUAL 或 AUTO
                + "short_window_days int not null,"               // 短期窗口天数（如 7 天）
                + "long_window_days int not null,"                // 长期窗口天数（如 30 天）
                + "cart_preference_weight decimal(6,4) not null," // 购物车偏好权重
                + "hot_weights_json text not null,"               // 热销权重（JSON 格式）
                + "anonymous_weights_json text not null,"         // 匿名用户权重（JSON 格式）
                + "personalized_weights_json text not null,"      // 个性化权重（JSON 格式）
                + "hot_signal_weights_json text not null,"        // 热销信号权重（JSON 格式）
                + "last_auto_tuned_at datetime null,"             // 上次自动调优时间
                + "create_time datetime default current_timestamp,"
                + "update_time datetime default current_timestamp on update current_timestamp"
                + ")");

        // 【教学】商品浏览事件表
        // 记录用户的商品浏览行为，用于计算热度和个性化推荐
        jdbcTemplate.execute("create table if not exists product_view_event ("
                + "id bigint primary key auto_increment,"
                + "product_id bigint not null,"        // 被浏览的商品 ID
                + "user_id bigint null,"               // 浏览用户 ID（null 表示匿名用户）
                + "source varchar(32) not null,"       // 浏览来源（如 homepage、search、detail）
                + "view_date date not null,"           // 浏览日期（用于按天聚合）
                + "viewed_at datetime default current_timestamp,"
                + "key idx_product_view_event_product_date (product_id, view_date),"  // 复合索引
                + "key idx_product_view_event_user_date (user_id, view_date)"         // 复合索引
                + ")");

        // 【教学】商品每日指标表
        // 按天聚合商品的各项指标，用于计算热度得分
        jdbcTemplate.execute("create table if not exists product_metric_daily ("
                + "id bigint primary key auto_increment,"
                + "metric_date date not null,"                        // 统计日期
                + "product_id bigint not null,"                       // 商品 ID
                + "view_count int not null default 0,"                // 浏览次数
                + "cart_add_count int not null default 0,"            // 加购次数
                + "paid_order_count int not null default 0,"          // 支付订单数
                + "paid_quantity int not null default 0,"             // 支付商品数量
                + "paid_amount decimal(12,2) not null default 0.00," // 支付金额
                + "create_time datetime default current_timestamp,"
                + "update_time datetime default current_timestamp on update current_timestamp,"
                + "unique key uk_product_metric_daily_date_product (metric_date, product_id),"  // 唯一约束
                + "key idx_product_metric_daily_date (metric_date)"
                + ")");
    }

    /**
     * 【教学】确保默认配置存在
     * <p>
     * 如果数据库中没有展示策略配置，则插入一条默认配置。
     * 默认配置的值来自 ShowcaseProperties（application.yml 中的配置）。
     */
    private void ensureDefaultConfig() {
        // 查询当前配置
        ShowcaseStrategyConfig current = showcaseStrategyConfigMapper.selectCurrent();
        if (current != null) {
            return; // 配置已存在，不需要初始化
        }

        // 创建默认配置
        ShowcaseStrategyConfig config = new ShowcaseStrategyConfig();
        config.setId(1L);                                    // 固定 ID（单条配置）
        config.setMode("MANUAL");                            // 默认手动模式
        config.setShortWindowDays(7);                        // 短期窗口 7 天
        config.setLongWindowDays(30);                        // 长期窗口 30 天
        config.setCartPreferenceWeight(BigDecimal.valueOf(showcaseProperties.getCartPreferenceWeight()));
        config.setHotWeightsJson(writeJson(showcaseProperties.getHot()));
        config.setAnonymousWeightsJson(writeJson(showcaseProperties.getAnonymous()));
        config.setPersonalizedWeightsJson(writeJson(showcaseProperties.getPersonalized()));
        config.setHotSignalWeightsJson(writeJson(showcaseProperties.getHotSignal()));
        showcaseStrategyConfigMapper.upsert(config);
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 要序列化的对象
     * @return JSON 字符串
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize showcase strategy defaults", ex);
        }
    }
}
