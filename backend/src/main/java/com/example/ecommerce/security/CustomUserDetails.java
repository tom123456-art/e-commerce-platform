package com.example.ecommerce.security;

import com.example.ecommerce.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 自定义用户详情类，实现 Spring Security 的 UserDetails 接口。
 * 将数据库 User 实体转换为 Spring Security 能理解的格式。
 * <p>
 * 📋 复制粘贴文件：从 02-code/11-Security/ 复制到项目中 security/ 目录。
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Integer status;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String username, String password,
                             Integer status, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.status = status;
        this.authorities = authorities;
    }

    /**
     * 工厂方法：从 User 实体创建 CustomUserDetails。
     * 角色映射规则（数据库 role 字段 -> Spring Security 权限）：
     * - 所有用户默认拥有 ROLE_USER（基础权限）
     * - role="ADMIN"（不区分大小写）额外添加 ROLE_ADMIN
     * - role="MERCHANT"（不区分大小写）额外添加 ROLE_MERCHANT
     */
    public static CustomUserDetails fromUser(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (user != null && "MERCHANT".equalsIgnoreCase(user.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
        }
        return new CustomUserDetails(
                user.getId(), user.getUsername(), user.getPassword(),
                user.getStatus(), authorities
        );
    }

    /**
     * 工厂方法：从已编码密码创建 CustomUserDetails 实例。
     * 此工厂方法在密码自动升级时使用，避免重复编码。
     */
    public static CustomUserDetails fromUserAndEncodedPassword(String username, String encodedPassword,
                                                               Collection<? extends GrantedAuthority> authorities) {
        return new CustomUserDetails(null, username, encodedPassword, 1, authorities);
    }

    public Long getId() {
        return id;
    }

    public boolean isAdmin() {
        return authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public boolean isMerchant() {
        return authorities.stream().anyMatch(a -> "ROLE_MERCHANT".equals(a.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == null || status >= 0;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == null || status == 1;
    }
}
