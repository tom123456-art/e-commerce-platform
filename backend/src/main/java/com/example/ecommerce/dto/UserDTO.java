package com.example.ecommerce.dto;

import java.util.Date;

/**
 * 用户数据传输对象（排除密码字段）。
 * <p>
 * 设计原则：
 * - 安全性：User 实体包含 password 字段，直接返回给前端会泄露密码哈希。
 * UserDTO 通过"只复制需要暴露的字段"来隔离敏感数据，确保密码永远不会出现在 API 响应中。
 * - 最小暴露：只包含前端展示和表单回填需要的字段，不暴露内部审计字段（如 deleted 等）。
 * - 序列化友好：提供无参构造器 + 全参构造器 + 完整 getter/setter，
 * 确保 Jackson 序列化/反序列化均能正常工作。
 */
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String role;        // USER / ADMIN / MERCHANT
    private Integer status;     // 1=启用, 0=禁用
    private Date createTime;
    private Date updateTime;

    /**
     * 无参构造器：Jackson 反序列化需要（如接收前端提交的用户编辑表单）
     */
    public UserDTO() {
    }

    /**
     * 全参构造器：从 User 实体转换时使用
     */
    public UserDTO(Long id, String username, String nickname, String email, String phone,
                   String role, Integer status, Date createTime, Date updateTime) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
