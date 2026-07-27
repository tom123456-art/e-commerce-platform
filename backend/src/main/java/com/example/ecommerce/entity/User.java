package com.example.ecommerce.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户实体类
 * @TableName user 将数据库的User表映射为User对象
 * @Data Lombok注解，在编译时会自动生成getter/setter/equals/toString/hashcode
 * Serializable：使对象可序列化为字节流，用来存入redis和网络传输的时候使用
 */
@Data
public class User implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * BCrypt哈希存储，不能使用明文存储
     * 序列化对象/Json的时候忽略这个字段，后端返回Json的内容中不会包含密码
     * 但是反序列化的前后端可以正常接收，前端注册或登录的时候，可以传入密码
     */
    private String password;

    /**
     * 
     */
    private String nickname;

    /**
     * 
     */
    private String email;

    /**
     * 
     */
    private String phone;

    /**
     * 
     */
    private String role;

    /**
     * 用户状态：1.启用，0禁用
     */
    private Integer status;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}