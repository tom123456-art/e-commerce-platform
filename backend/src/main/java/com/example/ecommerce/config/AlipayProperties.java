package com.example.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付配置属性类。
 *
 * 【注册方式】本类没有 @Component，而是在 SecurityConfig 中通过
 *   @EnableConfigurationProperties(AlipayProperties.class) 注册，
 * 等价于 @Component + @ConfigurationProperties。
 *
 * 【配置前缀】ecommerce.payment.alipay
 *
 * 【沙箱 vs 正式环境】
 *   沙箱网关：https://openapi-sandbox.dl.alipaydev.com/gateway.do
 *   正式网关：https://openapi.alipay.com/gateway.do
 *
 * 【Mock 模式】mockEnabled=true 时不调用真实支付宝 API，本地模拟支付流程，
 *   适用于前端开发、自动化测试、教学演示。
 */
@Component
@ConfigurationProperties(prefix = "ecommerce.payment.alipay")
public class AlipayProperties {

    private boolean enabled;             // 是否启用支付宝支付功能
    private boolean mockEnabled = true;  // 是否启用 Mock 模式（默认启用，便于教学）
    private String appId;                // 支付宝应用 ID
    private String privateKey;           // 应用私钥（RSA 签名用，绝不能泄露）
    private String publicKey;            // 支付宝公钥（验签用）
    private String gatewayUrl;           // 支付宝网关地址
    private String notifyUrl;            // 异步通知 URL（支付完成后支付宝主动 POST 这里）
    private String returnUrl;            // 同步跳转 URL（用户支付后浏览器跳到这里）

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isMockEnabled() { return mockEnabled; }
    public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
}
