package com.example.ecommerce.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * 【教学重点】Redis 工具类 —— 缓存操作的统一封装
 * ============================================================
 *
 * <h2>1. 什么是 Redis？为什么用它？</h2>
 * <p>Redis（Remote Dictionary Server）是一个开源的内存数据结构存储系统，
 * 可以用作数据库、缓存和消息中间件。</p>
 *
 * <p><b>在本项目中的用途</b>：</p>
 * <ul>
 *   <li><b>Token 存储</b>：用户登录后生成的 Token 存储在 Redis 中，
 *       设置过期时间实现自动登出</li>
 *   <li><b>数据缓存</b>：热点数据（如商品列表、分类信息）缓存到 Redis，
 *       减少数据库查询压力</li>
 *   <li><b>会话管理</b>：存储用户的会话信息</li>
 * </ul>
 *
 * <h2>2. 为什么需要封装 RedisTemplate？</h2>
 * <p>Spring 提供的 {@code RedisTemplate} 功能强大但 API 较为繁琐：</p>
 * <pre>{@code
 * // 直接使用 RedisTemplate（繁琐）
 * redisTemplate.opsForValue().set("key", "value", 30, TimeUnit.MINUTES);
 *
 * // 使用 RedisUtil 封装后（简洁）
 * redisUtil.set("key", "value", 1800);  // 1800 秒
 * }</pre>
 * <p>封装的好处：</p>
 * <ul>
 *   <li><b>简化调用</b>：隐藏底层 API 细节</li>
 *   <li><b>统一时间单位</b>：全部使用秒，避免分钟/小时/天的混淆</li>
 *   <li><b>易于替换</b>：如果将来换缓存方案，只需修改 RedisUtil 一处</li>
 * </ul>
 *
 * <h2>3. Redis 核心概念</h2>
 * <ul>
 *   <li><b>Key-Value</b>：Redis 的基本数据模型，每个数据都有一个唯一的键</li>
 *   <li><b>过期时间</b>：可以为每个键设置 TTL（Time To Live），
 *       到期后自动删除，非常适合做缓存和 Token 管理</li>
 *   <li><b>SCAN</b>：安全的键遍历命令，不会像 {@code KEYS *} 那样阻塞服务器</li>
 * </ul>
 *
 * @author 教学示例
 */
@Component
public class RedisUtil {

    /**
     * Spring Data Redis 提供的模板类，封装了 Redis 的各种操作。
     *
     * <p><b>教学点</b>：</p>
     * <ul>
     *   <li>{@code @Autowired}：自动注入 Spring 容器中的 {@code RedisTemplate} Bean</li>
     *   <li>泛型 {@code <String, Object>}：键为 String 类型，值为任意类型
     *       （通过配置的序列化器自动序列化/反序列化）</li>
     * </ul>
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 写入操作 ====================

    /**
     * 写入一个永久有效的键值对。
     *
     * <p><b>使用场景</b>：不需要过期的数据，如系统配置、字典数据等。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 缓存系统配置
     * redisUtil.set("config:max_upload_size", 10485760);
     * }</pre>
     *
     * <p><b>教学点</b>：永久有效的数据要谨慎使用，避免内存无限增长。
     * 大多数缓存场景应该使用带过期时间的版本。</p>
     *
     * @param key   Redis 键，建议使用冒号分隔的命名空间（如 {@code user:token:xxx}）
     * @param value Redis 值，会通过配置的序列化器自动序列化
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入一个带过期时间的键值对。
     *
     * <p><b>使用场景</b>：缓存数据、Token 存储等需要自动过期的场景。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 缓存商品详情，30 分钟后过期
     * redisUtil.set("product:detail:1001", product, 1800);
     *
     * // 存储用户 Token，7 天后过期
     * redisUtil.set("user:token:" + userId, token, 604800);
     * }</pre>
     *
     * <p><b>教学点</b>：过期时间单位统一为秒，内部转换为 {@link TimeUnit#SECONDS}。
     * 常用时间换算：1 分钟=60 秒，1 小时=3600 秒，1 天=86400 秒。</p>
     *
     * @param key     Redis 键
     * @param value   Redis 值
     * @param timeout 过期时间，单位为秒
     */
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    // ==================== 读取操作 ====================

    /**
     * 读取指定键对应的值。
     *
     * <p><b>使用场景</b>：获取缓存数据、验证 Token 等。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 获取缓存的商品详情
     * Product product = (Product) redisUtil.get("product:detail:1001");
     * if (product == null) {
     *     // 缓存未命中，从数据库查询
     *     product = productService.findById(1001);
     *     redisUtil.set("product:detail:1001", product, 1800);
     * }
     * }</pre>
     *
     * <p><b>教学点</b>：返回值为 {@code Object}，需要强制类型转换。
     * 这是 {@code RedisTemplate<String, Object>} 泛型的限制。
     * 如果类型不匹配会抛出 {@code ClassCastException}。</p>
     *
     * @param key Redis 键
     * @return 键对应的值；键不存在时返回 {@code null}
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // ==================== 判断操作 ====================

    /**
     * 判断指定键是否存在。
     *
     * <p><b>使用场景</b>：检查 Token 是否有效、检查缓存是否存在等。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 验证 Token 是否有效
     * boolean isValid = redisUtil.exists("user:token:" + userId);
     * }</pre>
     *
     * @param key Redis 键
     * @return 存在返回 {@code true}，否则返回 {@code false}
     */
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除单个键。
     *
     * <p><b>使用场景</b>：用户登出时删除 Token、清除特定缓存等。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 用户登出，删除 Token
     * redisUtil.delete("user:token:" + userId);
     * }</pre>
     *
     * <p><b>教学点</b>：删除不存在的键不会报错，Redis 会静默处理。</p>
     *
     * @param key Redis 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除多个键。
     *
     * <p><b>使用场景</b>：清除一批相关的缓存（如清除某个分类下的所有商品缓存）。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 清除一批缓存
     * List<String> keys = Arrays.asList("product:1", "product:2", "product:3");
     * redisUtil.delete(keys);
     * }</pre>
     *
     * @param keys 待删除的键集合
     */
    public void delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys);
    }

    // ==================== 过期控制 ====================

    /**
     * 为指定键设置过期时间。
     *
     * <p><b>使用场景</b>：为已存在的键延长或缩短过期时间。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 用户活跃时延长 Token 有效期
     * redisUtil.expire("user:token:" + userId, 604800); // 重新设置为 7 天
     * }</pre>
     *
     * @param key     Redis 键
     * @param timeout 过期时间，单位为秒
     */
    public void expire(String key, long timeout) {
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    // ==================== 模式匹配 ====================

    /**
     * 使用 {@code SCAN} 命令按模式遍历键集合。
     *
     * <p><b>教学点</b>：为什么用 SCAN 而不是 KEYS？</p>
     * <ul>
     *   <li><b>KEYS *</b>：一次性返回所有匹配的键，如果键数量很大（百万级），
     *       会阻塞 Redis 服务器，导致其他请求超时</li>
     *   <li><b>SCAN</b>：分批返回结果，每次只处理少量数据，不会阻塞服务器。
     *       适合生产环境使用</li>
     * </ul>
     *
     * <p><b>模式语法</b>：</p>
     * <ul>
     *   <li>{@code *} —— 匹配任意字符</li>
     *   <li>{@code ?} —— 匹配单个字符</li>
     *   <li>{@code [abc]} —— 匹配括号内的任意字符</li>
     * </ul>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 查找所有商品缓存键
     * Set<String> productKeys = redisUtil.keys("product:*");
     *
     * // 查找所有用户的 Token
     * Set<String> tokenKeys = redisUtil.keys("user:token:*");
     * }</pre>
     *
     * @param pattern 键模式，例如 {@code order:*}、{@code user:token:*}
     * @return 命中的键集合，不会返回 null
     */
    public Set<String> keys(String pattern) {
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(200)  // 每次扫描 200 个键
                    .build();
            Set<String> matchedKeys = new LinkedHashSet<>();
            // 教学点：使用 try-with-resources 自动关闭 Cursor
            try (var cursor = connection.keyCommands().scan(scanOptions)) {
                while (cursor.hasNext()) {
                    matchedKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return matchedKeys;
        });
        return keys == null ? Collections.emptySet() : keys;
    }

    /**
     * 删除匹配指定模式的全部键。
     *
     * <p><b>使用场景</b>：批量清除某类缓存。</p>
     *
     * <p><b>示例</b>：</p>
     * <pre>{@code
     * // 清除所有商品缓存
     * redisUtil.deleteByPattern("product:*");
     *
     * // 清除所有用户 Token（强制所有用户重新登录）
     * redisUtil.deleteByPattern("user:token:*");
     * }</pre>
     *
     * <p><b>注意</b>：在生产环境谨慎使用，大量键删除可能影响性能。</p>
     *
     * @param pattern 键模式
     */
    public void deleteByPattern(String pattern) {
        delete(keys(pattern));
    }

    /**
     * 原子递增计数器。
     * <p>用于限流计数器、点赞计数等需要原子性的场景。</p>
     *
     * @param key Redis 键
     * @return 递增后的值
     */
    public long increment(String key) {
        Long result = redisTemplate.opsForValue().increment(key);
        return result != null ? result : 0;
    }
}
