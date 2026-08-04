package com.example.ecommerce.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 兼容历史数据的密码编码器。
 * 支持 BCrypt 加密，兼容明文密码的渐进式迁移。
 *
 * 📋 复制粘贴文件：从 02-code/11-Security/ 复制到项目中 security/ 目录。
 */
public class LegacyCompatiblePasswordEncoder implements PasswordEncoder {

    private static final Logger log = LoggerFactory.getLogger(LegacyCompatiblePasswordEncoder.class);
    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();
    private final boolean allowPlainText;

    public LegacyCompatiblePasswordEncoder() { this(false); }

    public LegacyCompatiblePasswordEncoder(boolean allowPlainText) {
        this.allowPlainText = allowPlainText;
        if (allowPlainText) log.warn("[安全告警] 明文密码支持已启用，仅限迁移期间使用");
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) return false;
        if (isBcrypt(encodedPassword)) return delegate.matches(rawPassword, encodedPassword);
        if (allowPlainText) {
            log.warn("[安全告警] 检测到明文密码验证，请尽快迁移到BCrypt加密！");
            return encodedPassword.equals(rawPassword == null ? null : rawPassword.toString());
        }
        log.warn("[安全告警] 明文密码验证被拒绝，请使用BCrypt加密密码");
        return false;
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return !isBcrypt(encodedPassword);
    }

    private boolean isBcrypt(String encodedPassword) {
        return encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$");
    }
}
