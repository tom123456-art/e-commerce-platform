package com.example.ecommerce.security;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义用户详情服务，连接数据库与 Spring Security 认证体系。
 * <p>
 * 实现 UserDetailsService：根据用户名加载用户信息。
 * 实现 UserDetailsPasswordService：支持密码自动升级。
 * <p>
 * 📋 复制粘贴文件：从 02-code/11-Security/ 复制到项目中 security/ 目录。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return CustomUserDetails.fromUser(user);
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        User existingUser = userService.getUserByUsername(user.getUsername());
        if (existingUser != null) {
            userService.updatePassword(existingUser.getId(), newPassword);
            CustomUserDetails existing = CustomUserDetails.fromUser(existingUser);
            return CustomUserDetails.fromUserAndEncodedPassword(existingUser.getUsername(), newPassword, existing.getAuthorities());
        }
        return user;
    }
}
