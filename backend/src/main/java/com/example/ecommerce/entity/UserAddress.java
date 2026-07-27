package com.example.ecommerce.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName user_address
 */
@Data
public class UserAddress implements Serializable {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private String receiver;

    /**
     * 
     */
    private String phone;

    /**
     * 
     */
    private String province;

    /**
     * 
     */
    private String city;

    /**
     * 
     */
    private String district;

    /**
     * 
     */
    private String detailAddress;

    /**
     * 
     */
    private Integer isDefault;

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