package com.example.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体类
 *
 * @TableName user 将数据库的User表映射为User对象
 * @Data Lombok注解，在编译时会自动生成getter/setter/equals/tostring/hashcode
 * Serializable: 使对象可序列化为字节流，用来存入redis和网络传输的时候使用
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 用户主键ID
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 用户密码,BCrypt哈希存储，不能使用明文存储
     * 序列化对象/JSON的时候忽略这个字段，后端返回JSON的内容中不会包含密码
     * 但是反序列化的时候可以正常接收，前端注册或登录的时候，可以传入密码
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /**
     * 用户昵称
     */
    private String nickname;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 用户电话
     */
    private String phone;
    /**
     * 用户角色标识，管理员、商家、普通用户
     */
    private String role;
    /**
     * 用户状态：1启用，0禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}