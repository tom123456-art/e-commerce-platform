package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "用户接口", description = "用户管理接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails currentUser) {
            if (!currentUser.isAdmin() && !currentUser.getId().equals(id)) {
                return Result.failure(Result.FORBIDDEN_CODE, "无权限访问");
            }
        }
        return Result.success(toUserDTO(userService.getUserById(id)));
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username
     * @return
     */
    @GetMapping("/username/{username}")
    public Result<UserDTO> getByUsername(@PathVariable String username) {
        return Result.success(toUserDTO(userService.getUserByUsername(username)));
    }

    /**
     * 获取所有用户（仅 ADMIN）—— 管理后台用户管理页调用
     *
     * @return
     */
    @GetMapping
    public Result<List<UserDTO>> getAll() {
        return Result.success(userService.getAll().stream()
                .map(this::toUserDTO).collect(Collectors.toList()));
    }

    /**
     * 创建用户
     *
     * @param user
     * @return
     */
    @PostMapping
    public Result<Void> save(@Valid @RequestBody User user) {
        userService.save(user);
        return Result.success();
    }

    /**
     * 更新用户信息
     *
     * @param user
     * @return
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody User user) {
        userService.updateUser(user);
        return Result.success();
    }

    /**
     * 删除用户（仅 ADMIN）—— 管理后台用户管理页调用
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }


    /**
     * 把User实体类转换为UserDTO(排除密码字段)
     *
     * @param user
     * @return
     */
    private UserDTO toUserDTO(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(), user.getUsername(), user.getNickname(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getCreateTime(), user.getUpdateTime()
        );
    }
}
