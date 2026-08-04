package com.example.ecommerce.utils;

/**
 * 敏感信息脱敏工具类 —— 日志中记录用户信息时的隐私保护。
 *
 * 设计原则：
 *   1. 纯静态方法：工具类无状态，所有方法 static
 *   2. null 安全：输入 null 时返回 null，不抛异常
 *   3. 短字符串保护：输入太短无法安全脱敏时，返回原值
 *
 * 使用场景：
 *   log.info("用户登录，手机号：{}", SensitiveDataUtil.maskPhone(phone));  // 138****5678
 *   log.info("发送邮件到：{}", SensitiveDataUtil.maskEmail(email));       // u****@example.com
 *
 * 脱敏规则：
 *   手机号：138****5678      （保留前3后4）
 *   邮箱：  u****@example.com （保留首字符和域名）
 *   密码：  ******           （完全隐藏，固定6个星号，不泄露长度）
 *   Token： eyJhbG****c123   （保留前6后4）
 *   身份证：110***********1234（保留前3后4）
 */
public class SensitiveDataUtil {

    /**
     * 手机号脱敏：保留前 3 位和后 4 位。
     * 13812345678 → 138****5678
     * 长度不足 7 时返回原值（无法安全脱敏）。
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏：保留 @ 前第一个字符和域名部分。
     * user@example.com → u****@example.com
     * @ 前只有 0 或 1 个字符时返回原值。
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "****" + email.substring(atIndex);
    }

    /**
     * 密码脱敏：统一输出 6 个星号。
     * 无论原始密码多长，都输出 "******"。
     * 不泄露密码长度，避免攻击者推测。
     */
    public static String maskPassword(String password) {
        if (password == null) {
            return null;
        }
        return "******";
    }

    /**
     * Token 脱敏：保留前 6 位和后 4 位。
     * eyJhbGci...abc123 → eyJhbG****c123
     * 长度 <= 10 时返回 6 个星号（太短完全隐藏）。
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return token == null ? null : "******";
        }
        return token.substring(0, 6) + "****" + token.substring(token.length() - 4);
    }

    /**
     * 身份证号脱敏：保留前 3 位和后 4 位。
     * 110101199001011234 → 110***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 通用脱敏方法：自定义保留前缀和后缀长度。
     * mask("6222021234567890123", 4, 4, '*') → "6222***********0123"（银行卡号）
     * mask("ABCDEFGH", 2, 2, '#')             → "AB####GH"
     */
    public static String mask(String value, int prefixLen, int suffixLen, char maskChar) {
        if (value == null) {
            return null;
        }
        int totalMask = value.length() - prefixLen - suffixLen;
        if (totalMask <= 0) {
            return value;  // 字符串太短，无法脱敏
        }
        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, prefixLen);  // 保留前缀
        for (int i = 0; i < totalMask; i++) {
            sb.append(maskChar);         // 填充隐藏字符
        }
        sb.append(value.substring(value.length() - suffixLen));  // 保留后缀
        return sb.toString();
    }
}
