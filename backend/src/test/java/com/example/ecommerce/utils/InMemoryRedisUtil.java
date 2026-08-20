package com.example.ecommerce.utils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [TEST-BEAN] 内存版 RedisUtil，仅用于测试环境。
 *
 * <p>作用：@WebMvcTest 切片测试不会加载真实的 Redis 连接（没有 RedisTemplate），
 * 而全局限流过滤器 RateLimitFilter 依赖 RedisUtil。此实现基于 ConcurrentHashMap
 * 模拟常用命令（set/get/exists/delete/expire/keys/increment/deleteByPattern），
 * 行为与真实 Redis 近似（含原子递增、模式删除），不再需要 mock 或连接真实 Redis。</p>
 *
 * <p>注意：这是测试专用实现，不添加 @Component，由 {@code TestRedisConfig} 以 @Bean 提供。</p>
 */
public class InMemoryRedisUtil extends RedisUtil {

    private final Map<String, Object> store = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public void set(String key, Object value) {
        store.put(key, value);
    }

    @Override
    public void set(String key, Object value, long timeout) {
        store.put(key, value);
    }

    @Override
    public Object get(String key) {
        return store.get(key);
    }

    @Override
    public boolean exists(String key) {
        return store.containsKey(key);
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        counters.remove(key);
    }

    @Override
    public void delete(Collection<String> keys) {
        if (keys == null) {
            return;
        }
        keys.forEach(this::delete);
    }

    @Override
    public void expire(String key, long timeout) {
        // 内存实现不处理过期，仅占位
    }

    @Override
    public Set<String> keys(String pattern) {
        Set<String> result = new LinkedHashSet<>();
        for (String key : store.keySet()) {
            if (matches(key, pattern)) {
                result.add(key);
            }
        }
        return result;
    }

    @Override
    public void deleteByPattern(String pattern) {
        delete(keys(pattern));
    }

    @Override
    public long increment(String key) {
        return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 简易 glob 匹配：支持单个 '*' 作为前缀/后缀通配符
     */
    private boolean matches(String key, String pattern) {
        if (pattern == null) {
            return false;
        }
        int idx = pattern.indexOf('*');
        if (idx < 0) {
            return key.equals(pattern);
        }
        String prefix = pattern.substring(0, idx);
        String suffix = pattern.substring(idx + 1);
        if (!prefix.isEmpty() && !key.startsWith(prefix)) {
            return false;
        }
        return suffix.isEmpty() || key.endsWith(suffix);
    }
}
