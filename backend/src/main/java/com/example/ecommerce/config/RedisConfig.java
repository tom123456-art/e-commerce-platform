package com.example.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// =============================================================================
// 【教学】Redis 配置类
// =============================================================================
//
// 一、Redis 是什么？
// ---------------------------------
// Redis（Remote Dictionary Server）是一个开源的内存数据结构存储系统，常用作：
//   1. 缓存（Cache）：将热点数据存在内存中，减少数据库查询
//   2. 会话存储（Session Store）：分布式系统中共享用户会话
//   3. 消息队列（Message Queue）：轻量级的发布/订阅
//   4. 分布式锁（Distributed Lock）：协调多实例之间的并发控制
//
// 在本项目中 Redis 的用途：
//   - Token 存储：用户登录后生成的 Token 存在 Redis 中，用于验证请求合法性
//   - 商品缓存：缓存商品列表和详情，减少数据库压力
//   - 限流计数：记录 API 请求频率，防止接口被滥用
//   - 认证缓存：缓存用户权限信息
//
// 二、RedisTemplate 是什么？
// ---------------------------------
// RedisTemplate 是 Spring Data Redis 提供的核心操作模板类，封装了 Redis 的各种操作：
//   - opsForValue()：操作字符串类型（String）
//   - opsForHash()：操作哈希类型（Hash）
//   - opsForList()：操作列表类型（List）
//   - opsForSet()：操作集合类型（Set）
//   - opsForZSet()：操作有序集合类型（Sorted Set）
//
// 三、序列化器（Serializer）详解
// ---------------------------------
// Redis 存储的是字节数据，Java 对象需要序列化后才能存入 Redis。
// 常见的序列化器有：
//   - StringRedisSerializer：将字符串直接转为字节，可读性最好（推荐用于 Key）
//   - Jackson2JsonJsonSerializer：将对象转为 JSON 字节，可读性好（推荐用于 Value）
//   - JdkSerializationRedisSerializer：Java 默认序列化，可读性差但通用（Spring Boot 默认）
//   - GenericJackson2JsonRedisSerializer：通用 JSON 序列化，带类型信息
//
// 本项目统一使用 StringRedisSerializer，因为：
//   1. Key 和 Value 都是简单字符串（Token、缓存 JSON）
//   2. 在 Redis CLI 中可以直接查看和调试数据
//   3. 避免 JDK 序列化的类名前缀污染数据
//
// 四、ApplicationRunner 是什么？
// ---------------------------------
// ApplicationRunner 是 Spring Boot 提供的应用启动后回调接口。
// 当 Spring 容器完全初始化后，所有 ApplicationRunner 实现的 run() 方法会被调用。
// 常见用途：
//   - 启动时检查外部依赖（数据库、Redis、MQ）是否可用
//   - 初始化缓存数据
//   - 执行数据迁移
//
// 五、@Profile("!test") 注解详解
// ---------------------------------
// @Profile 用于条件化地激活 Bean，只在指定的 Profile 下才生效。
// "!test" 表示"非 test 环境"，即在 dev、prod 等环境下生效，test 环境下跳过。
// 为什么？因为在测试环境中，Redis 可能不可用（或使用测试专用配置），
// 如果启动时执行健康检查会报错，影响测试执行。
//
// 六、@ConditionalOnProperty 注解详解
// ---------------------------------
// 这是 Spring Boot 提供的条件注解，根据 application.yml 中的配置项决定是否创建 Bean。
//   prefix = "ecommerce.redis"：配置项前缀
//   name = "startup-check-enabled"：配置项名称
//   havingValue = "true"：期望的值
//   matchIfMissing = true：如果配置项不存在，默认也创建 Bean
//
// 这意味着可以通过在 yml 中设置 ecommerce.redis.startup-check-enabled=false 来禁用启动检查。
// =============================================================================

@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * 【教学】配置 RedisTemplate Bean
     *
     * @param factory Redis 连接工厂（由 Spring Boot 自动配置创建）
     * @return 已完成序列化配置的 RedisTemplate
     * @Bean 注解告诉 Spring：这个方法的返回值是一个 Bean，需要注册到 Spring 容器中。
     * 方法参数 RedisConnectionFactory 由 Spring 自动注入（它在 spring-boot-starter-data-redis
     * 的自动配置类 RedisAutoConfiguration 中创建）。
     * <p>
     * RedisConnectionFactory 负责管理与 Redis 服务器的连接，底层使用 Lettuce 客户端库。
     * Spring Boot 自动配置会根据 application.yml 中的 spring.data.redis.* 配置创建连接工厂。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置连接工厂（告诉 RedisTemplate 如何连接 Redis 服务器）
        template.setConnectionFactory(factory);

        // 【教学】设置序列化器
        // Key 序列化器：Redis 中的 Key 使用字符串格式，便于在 Redis CLI 中查看
        template.setKeySerializer(new StringRedisSerializer());
        // Value 序列化器：Redis 中的 Value 也使用字符串格式
        template.setValueSerializer(new StringRedisSerializer());
        // Hash Key 序列化器：Hash 结构中的字段名使用字符串格式
        template.setHashKeySerializer(new StringRedisSerializer());
        // Hash Value 序列化器：Hash 结构中的字段值使用字符串格式
        template.setHashValueSerializer(new StringRedisSerializer());

        // 【教学】afterPropertiesSet() 的作用
        // 这是一个生命周期方法，在所有属性设置完成后调用。
        // 它会检查必要的属性（如 connectionFactory）是否已设置，
        // 如果缺少会抛出异常。这是一个良好的编程习惯——"尽早失败"（Fail Fast）。
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 配置 Redis 缓存管理器。
     *
     * <p>缓存策略：</p>
     * <ul>
     *   <li>默认 TTL 30 分钟</li>
     *   <li>不同缓存名称可配置不同 TTL</li>
     *   <li>缓存穿透防护：缓存 null 值（TTL 2 分钟）</li>
     *   <li>缓存雪崩防护：TTL 加随机偏移（±10%）</li>
     * </ul>
     *
     * @param factory Redis 连接工厂
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 默认配置：TTL 30 分钟，JSON 序列化
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues() // 不缓存 null（穿透防护由业务层处理）
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 各缓存名称的独立 TTL 配置
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("product:detail", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("categories:all", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("products:hot", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("products:recommend", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware() // 支持事务环境下缓存与数据库一致性
                .build();
    }

    /**
     * 【教学】Redis 启动健康检查
     * <p>
     * ApplicationRunner 接口的 run() 方法会在 Spring Boot 应用完全启动后自动调用。
     * 这里用它来检查 Redis 服务是否可用，如果不可用只记录警告（不阻止应用启动）。
     *
     * @param factory Redis 连接工厂
     * @return 应用启动后执行的 Redis 检查任务
     * @Profile("!test")：在测试环境下不执行此检查（避免测试环境没有 Redis 时报错）
     * @ConditionalOnProperty：允许通过配置项禁用此检查
     */
    @Bean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "ecommerce.redis", name = "startup-check-enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner redisStartupHealthChecker(RedisConnectionFactory factory) {
        return args -> {
            // 【教学】try-with-resources 语法
            // Redis Connection 实现了 Closeable 接口，使用 try-with-resources 确保连接在使用后自动归还连接池
            try (var connection = factory.getConnection()) {
                // PING 是 Redis 最简单的命令，用于测试连接是否正常。正常返回 "PONG"。
                String result = connection.ping();
                log.info("Redis startup check passed: {}", result == null ? "PONG" : result);
            } catch (Exception ex) {
                // 只记录警告，不抛出异常——Redis 不可用时应用仍可启动（缓存功能降级）
                log.warn("Redis startup check failed, cache features may be degraded: {}", ex.getMessage());
            }
        };
    }
}
