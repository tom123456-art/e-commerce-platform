package com.example.ecommerce.security;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.service.UserService;
import com.example.ecommerce.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义 Token 认证服务（核心安全组件）。
 * <p>
 * Token 格式：ECM.{rawToken}.{hmacSignature}
 * - ECM：固定前缀（E-Commerce Mall 缩写）
 * - rawToken：32 位十六进制随机字符串（UUID 去连字符）
 * - hmacSignature：HMAC-SHA256 签名的 Base64Url 编码
 * <p>
 * 双层缓存策略：
 * 一级缓存：本地 ConcurrentHashMap（JVM 内存）
 * 二级缓存：Redis（网络调用）
 * <p>
 * 📋 复制粘贴文件：从 03-code/security/ 或 02-code/11-Security/ 复制到项目中 security/ 目录。
 * 完整逐行注释见 [03-用户认证模块 §1.2.4](03-用户认证模块.md)。
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final String TOKEN_PREFIX = "ECM";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RedisUtil redisUtil;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long tokenExpireSeconds;
    private final byte[] hmacSecretKey;
    private final Map<String, SessionSnapshot> localSessions = new ConcurrentHashMap<>();

    public TokenService(RedisUtil redisUtil,
                        UserService userService,
                        @Value("${ecommerce.auth.token-expire-seconds:7200}") long tokenExpireSeconds,
                        @Value("${ecommerce.auth.token-hmac-secret}") String hmacSecretKey) {
        this.redisUtil = redisUtil;
        this.userService = userService;
        this.tokenExpireSeconds = tokenExpireSeconds;
        if (hmacSecretKey == null || hmacSecretKey.length() < 32) {
            throw new IllegalStateException("HMAC密钥长度不得少于32位");
        }
        this.hmacSecretKey = hmacSecretKey.getBytes(StandardCharsets.UTF_8);
    }

    public String createToken(CustomUserDetails userDetails) {
        cleanupExpiredLocalSessions();
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        SessionSnapshot snapshot = new SessionSnapshot(
                userDetails.getId(), userDetails.getUsername(),
                Instant.now().plusSeconds(tokenExpireSeconds).toEpochMilli()
        );
        storeSession(rawToken, snapshot);
        String signature = computeHmac(rawToken);
        return TOKEN_PREFIX + "." + rawToken + "." + signature;
    }

    public CustomUserDetails parseToken(String token) {
        if (token == null || token.trim().isEmpty()) return null;

        String normalizedToken = token.trim();
        if (!normalizedToken.startsWith(TOKEN_PREFIX + ".")) {
            log.warn("[安全告警] 收到无前缀的伪造Token尝试: {}",
                    normalizedToken.substring(0, Math.min(normalizedToken.length(), 20)));
            return null;
        }

        String[] parts = normalizedToken.split("\\.");
        if (parts.length != 3) {
            log.warn("[安全告警] Token格式异常，缺少签名部分");
            return null;
        }

        String rawToken = parts[1];
        String providedSignature = parts[2];

        String expectedSignature = computeHmac(rawToken);
        if (!java.security.MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("[安全告警] Token签名验证失败");
            return null;
        }

        cleanupExpiredLocalSessions();
        SessionSnapshot snapshot = readSession(rawToken);
        if (snapshot == null || snapshot.getExpireAt() < System.currentTimeMillis()) {
            revokeToken(rawToken);
            return null;
        }

        User user = userService.getUserByUsername(snapshot.getUsername());
        if (user == null) {
            revokeToken(rawToken);
            return null;
        }
        return CustomUserDetails.fromUser(user);
    }

    public void revokeToken(String token) {
        if (token == null || token.trim().isEmpty()) return;
        String rawToken = token.trim();
        if (rawToken.startsWith(TOKEN_PREFIX + ".")) {
            String[] parts = rawToken.split("\\.");
            if (parts.length >= 2) rawToken = parts[1];
        }
        localSessions.remove(rawToken);
        try {
            redisUtil.delete(buildTokenKey(rawToken));
        } catch (Exception ex) {
            log.warn("Failed to delete token from Redis: {}", ex.getMessage());
        }
    }

    private void storeSession(String token, SessionSnapshot snapshot) {
        localSessions.put(token, snapshot);
        try {
            redisUtil.set(buildTokenKey(token),
                    objectMapper.writeValueAsString(snapshot), tokenExpireSeconds);
        } catch (Exception ex) {
            log.warn("Failed to store token in Redis: {}", ex.getMessage());
        }
    }

    private SessionSnapshot readSession(String token) {
        SessionSnapshot local = localSessions.get(token);
        if (local != null && local.getExpireAt() >= System.currentTimeMillis()) return local;
        if (local != null) localSessions.remove(token, local);

        try {
            if (redisUtil.exists(buildTokenKey(token))) {
                String json = String.valueOf(redisUtil.get(buildTokenKey(token)));
                SessionSnapshot snapshot = objectMapper.readValue(json, SessionSnapshot.class);
                if (snapshot != null && snapshot.getExpireAt() >= System.currentTimeMillis()) {
                    localSessions.put(token, snapshot);
                    return snapshot;
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to read token from Redis: {}", ex.getMessage());
        }
        return null;
    }

    private void cleanupExpiredLocalSessions() {
        long now = System.currentTimeMillis();
        localSessions.entrySet().removeIf(e -> e.getValue() == null || e.getValue().getExpireAt() < now);
    }

    private String buildTokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacSecretKey, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC签名初始化失败", ex);
        }
    }

    public static class SessionSnapshot {
        private Long userId;
        private String username;
        private long expireAt;

        public SessionSnapshot() {
        }

        public SessionSnapshot(Long userId, String username, long expireAt) {
            this.userId = userId;
            this.username = username;
            this.expireAt = expireAt;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(long expireAt) {
            this.expireAt = expireAt;
        }
    }
}
