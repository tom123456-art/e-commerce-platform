package com.example.ecommerce.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Redis 工具类 —— 对 RedisTemplate 的简化封装。
 *
 * @Component：标记为 Spring 组件，可被其他类注入使用
 *
 * 封装的好处：
 *   1. 简化调用：隐藏 RedisTemplate 的繁琐 API
 *   2. 统一时间单位：支持自定义 TimeUnit
 *   3. 易于替换：将来换缓存方案，只需修改 RedisUtil 一处
 *
 * 使用示例：
 *   // 存储商品（1小时过期）
 *   redisUtil.set("product:" + id, product, 1, TimeUnit.HOURS);
 *
 *   // 获取商品（自动反序列化）
 *   Product product = redisUtil.get("product:" + id, Product.class);
 */
@Component
public class RedisUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 写入操作 ====================

    /**
     * 存储对象（序列化为 JSON）。
     * @param key 缓存键
     * @param value 缓存值（自动序列化为 JSON 字符串）
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, toJson(value));
    }

    /**
     * 存储对象并设置过期时间（单位：秒）。
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, toJson(value), timeout, TimeUnit.SECONDS);
    }

    /**
     * 存储对象并设置过期时间（自定义时间单位）。
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位（如 TimeUnit.HOURS、TimeUnit.MINUTES）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, toJson(value), timeout, unit);
    }

    // ==================== 读取操作 ====================

    /**
     * 获取缓存原始值（Object 类型）。
     * 返回 RedisTemplate 存储的原始对象，通常为 JSON 字符串。
     * 调用方可通过 String.valueOf() 转为字符串，再用 ObjectMapper 反序列化为目标类型。
     *
     * 使用示例：
     *   String json = String.valueOf(redisUtil.get("product:1"));
     *   Product product = objectMapper.readValue(json, Product.class);
     *
     * @param key 缓存键
     * @return 缓存原始值（通常为 JSON 字符串），不存在时返回 null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取对象（自动反序列化 JSON）。
     * @param key 缓存键
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 反序列化后的对象，缓存不存在时返回 null
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return fromJson(String.valueOf(value), clazz);
    }

    /**
     * 获取原始值（返回 JSON 字符串）。
     * 与 get(String) 的区别：本方法显式返回 String 类型，调用方无需再做 String.valueOf() 转换。
     * @param key 缓存键
     * @return JSON 字符串，缓存不存在时返回 null
     */
    public String getRaw(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : String.valueOf(value);
    }

    // ==================== 判断操作 ====================

    /**
     * 判断 key 是否存在。
     * @param key 缓存键
     * @return 存在返回 true
     */
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除单个键。
     * @param key 缓存键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除多个键。
     * @param keys 键集合
     */
    public void delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        redisTemplate.delete(keys);
    }

    // ==================== 过期控制 ====================

    /**
     * 为已存在的键设置过期时间（单位：秒）。
     * @param key 缓存键
     * @param timeout 过期时间（秒）
     */
    public void expire(String key, long timeout) {
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 为已存在的键设置过期时间（自定义时间单位）。
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    // ==================== 计数操作 ====================

    /**
     * 原子递增计数器（用于限流、计数等场景）。
     * @param key 缓存键
     * @return 递增后的值
     */
    public long increment(String key) {
        Long result = redisTemplate.opsForValue().increment(key);
        return result != null ? result : 0;
    }

    /**
     * 原子递减计数器。
     * @param key 缓存键
     * @return 递减后的值
     */
    public long decrement(String key) {
        Long result = redisTemplate.opsForValue().decrement(key);
        return result != null ? result : 0;
    }

    // ==================== 模式匹配 ====================

    /**
     * 使用 SCAN 命令按模式遍历键。
     *
     * 为什么用 SCAN 而不是 KEYS？
     *   - KEYS *：一次性返回所有匹配键，键数量大时（百万级）会阻塞 Redis 服务器
     *   - SCAN：分批返回结果，每次只处理少量数据，不会阻塞服务器，适合生产环境
     *
     * 模式语法：* 匹配任意字符，? 匹配单个字符，[abc] 匹配括号内任意字符
     *
     * @param pattern 键模式，如 "product:*"、"user:token:*"
     * @return 命中的键集合
     */
    public Set<String> keys(String pattern) {
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(200)  // 每次扫描 200 个键
                    .build();
            Set<String> matchedKeys = new LinkedHashSet<>();
            // try-with-resources 自动关闭 Cursor
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
     * 使用场景：批量清除某类缓存，如 redisUtil.deleteByPattern("product:*")
     * 注意：大量键删除可能影响性能，生产环境谨慎使用。
     */
    public void deleteByPattern(String pattern) {
        delete(keys(pattern));
    }

    // ==================== JSON 序列化/反序列化 ====================

    /**
     * 将对象序列化为 JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            if (value instanceof String) {
                return (String) value;
            }
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象。
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }
}
