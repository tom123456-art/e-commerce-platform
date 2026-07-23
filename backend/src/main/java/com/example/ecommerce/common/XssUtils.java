package com.example.ecommerce.common;

/**
 * XSS（跨站脚本攻击）防护工具类。
 * XSS 攻击：攻击者在输入框中注入 <script> 标签，当其他用户浏览时执行恶意脚本。
 * 防护方式：检测输入中是否包含危险的 HTML/JS 标签。
 */
public class XssUtils {

    /**
     * 检测输入是否包含 XSS 攻击内容。
     * @param input 待检测的字符串
     * @return 包含 XSS 内容返回 true
     */
    public static boolean containsXss(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:")
                || lower.contains("onerror") || lower.contains("onload")
                || lower.contains("onclick") || lower.contains("<iframe");
    }

    /**
     * 清理输入中的危险字符。
     * 将 HTML 特殊字符转义为实体编码。
     */
    public static String clean(String input) {
        if (input == null) return null;
        return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;");
    }
}