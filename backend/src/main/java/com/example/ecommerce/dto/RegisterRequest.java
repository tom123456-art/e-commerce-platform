package com.example.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO。
 *
 * 这是系统中"验证注解最丰富"的 DTO——注册是系统第一道防线，必须严格校验。
 * 本类集中展示了 Jakarta Validation 的多种注解用法。
 *
 * 与 User Entity 的关系：本类是 Entity 的"子集 + 扩展"：
 *   - 子集：不包含 id、role、status、createTime（由后端设置）
 *   - 扩展：增加 confirmPassword（确认密码，纯前端交互字段，Entity 中没有）
 */
public class RegisterRequest {

    /**
     * 用户名。
     * @NotBlank：非空且非纯空白
     * @Size(min=4, max=20)：长度 4-20 位
     * @Pattern：只允许字母、数字、下划线，防止注入和 URL 不安全字符
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需为 4-20 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅支持字母、数字和下划线")
    private String username;

    /**
     * 密码。
     * 正则使用零宽断言（Lookahead）强制包含大写、小写、数字、特殊字符四类。
     * (?=.*[a-z]) 表示"往后看是否存在小写字母"，不消耗字符。
     * 多个断言并列即"同时满足所有条件"。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!_.\\-])[A-Za-z\\d@#$%^&+=!_.\\-]+$",
            message = "密码需包含大写字母、小写字母、数字和特殊字符")
    private String password;

    /**
     * 确认密码（二次输入）。
     * 纯交互字段，数据库无对应列。
     * DTO 层只校验非空，一致性校验在 Service 层用 if 判断
     * （跨字段验证不适合用注解实现）。
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 昵称（显示名称），可重复，与用户名（登录账号）不同 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过 50 位")
    private String nickname;

    /** 邮箱，@Email 校验格式 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号，正则匹配中国大陆 11 位手机号（1 开头） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    // ========== Getter / Setter ==========
    // 注意：DTO 不使用 Lombok @Data，因为校验注解需要明确的 getter/setter

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}