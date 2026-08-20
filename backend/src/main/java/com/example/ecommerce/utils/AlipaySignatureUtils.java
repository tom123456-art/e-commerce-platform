package com.example.ecommerce.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付宝签名工具类 —— 实现 SHA256withRSA 签名和验签。
 * <p>
 * 支付安全的核心机制：
 * - 签名（sign）：我方发起支付请求时，用商户私钥对参数签名，支付宝用我方公钥验证
 * - 验签（verify）：支付宝回调通知时，用支付宝公钥验证回调数据的真实性
 * <p>
 * 签名流程：
 * 1. 将所有参数按 Key 字母序排序
 * 2. 拼接为 "key1=value1&key2=value2&..." 格式（排除 sign 和 sign_type）
 * 3. 用 SHA256withRSA 算法 + 私钥计算签名
 * 4. 签名结果 Base64 编码后放入 sign 参数
 * <p>
 * 设计原则：
 * - final 类 + 私有构造器：标准工具类模式，不可实例化
 * - 验签失败返回 false 而非抛异常：安全最佳实践（不泄露失败原因）
 * - null 安全：参数为空时返回合理默认值
 */
public final class AlipaySignatureUtils {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGORITHM = "RSA";

    private AlipaySignatureUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 对请求参数签名（发起支付时使用）。
     * 默认排除 "sign" 字段。
     *
     * @param params     待签名的参数 Map
     * @param privateKey 商户 RSA 私钥（PKCS8 格式，Base64 编码）
     * @return Base64 编码的签名字符串
     */
    public static String sign(Map<String, String> params, String privateKey) {
        return sign(params, privateKey, List.of("sign"));
    }

    /**
     * 对请求参数签名（可自定义排除字段）。
     *
     * @param params       待签名的参数 Map
     * @param privateKey   商户 RSA 私钥
     * @param excludedKeys 需要排除的字段（如 sign、sign_type）
     * @return Base64 编码的签名字符串
     */
    public static String sign(Map<String, String> params, String privateKey, List<String> excludedKeys) {
        try {
            // 第一步：构建待签名字符串（按 Key 排序，排除指定字段）
            String signContent = buildSignContent(params, excludedKeys);
            // 第二步：用私钥计算 SHA256withRSA 签名
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(getPrivateKey(privateKey));
            signature.update(signContent.getBytes(StandardCharsets.UTF_8));
            // 第三步：Base64 编码签名结果
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("支付宝签名失败", e);
        }
    }

    /**
     * 验证支付宝回调签名（支付回调时使用）。
     * 默认排除 "sign" 和 "sign_type" 字段。
     *
     * @param params    回调参数 Map（含 sign 字段）
     * @param publicKey 支付宝 RSA 公钥（X509 格式，Base64 编码）
     * @return 验签通过返回 true，失败返回 false（不抛异常）
     */
    public static boolean verify(Map<String, String> params, String publicKey) {
        try {
            String sign = params.get("sign");
            if (sign == null || sign.isEmpty()) {
                return false;
            }
            // 构建待验签字符串（排除 sign 和 sign_type）
            String signContent = buildSignContent(params, List.of("sign", "sign_type"));
            // 用支付宝公钥验证签名
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(getPublicKey(publicKey));
            signature.update(signContent.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            // 安全最佳实践：验签失败返回 false，不泄露具体错误原因
            return false;
        }
    }

    /**
     * 构建 URL 查询字符串（用于拼接支付请求 URL）。
     * 参数按 Key 排序，值做 URL 编码。
     *
     * @param params 参数 Map
     * @return 格式如 "key1=value1&key2=value2"
     */
    public static String buildQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * 构建待签名/验签的原始字符串。
     * 规则：按 Key 字母序排序，拼接为 "key=value&key=value" 格式（不做 URL 编码）。
     *
     * @param params       参数 Map
     * @param excludedKeys 需要排除的字段
     * @return 待签名字符串
     */
    public static String buildSignContent(Map<String, String> params, List<String> excludedKeys) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> excludedKeys == null || !excludedKeys.contains(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 将 Base64 编码的私钥字符串解析为 PrivateKey 对象（PKCS8 格式）
     */
    private static PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(cleanPem(privateKeyStr));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 将 Base64 编码的公钥字符串解析为 PublicKey 对象（X509 格式）
     */
    private static PublicKey getPublicKey(String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(cleanPem(publicKeyStr));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 清理 PEM 格式密钥：去掉头尾标记和换行符
     */
    private static String cleanPem(String pem) {
        return pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");
    }

    /**
     * URL 编码（UTF-8），将 + 替换为 %20（支付宝要求）
     */
    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
