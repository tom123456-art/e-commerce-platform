package com.example.ecommerce.config;

import com.example.ecommerce.utils.InMemoryRedisUtil;
import com.example.ecommerce.utils.RedisUtil;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * [TEST-BEAN] 测试用 Redis 配置。
 *
 * <p>提供内存版 {@link RedisUtil} Bean，供需要 RedisUtil 的 Web 切片测试使用
 * （典型如 RateLimitFilter）。加 @Primary 以避免与主应用 @Component RedisUtil 冲突。</p>
 *
 * <p>用法：在测试类上 {@code @Import(TestRedisConfig.class)} 即可，无需再 @MockitoBean RedisUtil。</p>
 */
@TestConfiguration
public class TestRedisConfig {

    /**
     * 内存版 RedisUtil，真实模拟 set/get/increment/keys 等行为，不连接真实 Redis
     */
    @Bean
    @Primary
    public RedisUtil redisUtil() {
        return new InMemoryRedisUtil();
    }

    /**
     * [TEST-BEAN] 仅用于满足 RedisUtil 父类中 {@code @Autowired RedisTemplate} 的注入要求。
     * InMemoryRedisUtil 并不实际使用 RedisTemplate，故用 mock 即可，无需真实 Redis 连接。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
}
