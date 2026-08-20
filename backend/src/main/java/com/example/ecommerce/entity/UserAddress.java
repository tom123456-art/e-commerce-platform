package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @TableName user_address
 */
@Data
public class UserAddress implements Serializable {
    private static final long serialVersionUID = 1L;
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

    public String getFullAddress() {
        String stringBuilder = province.trim() +
                city.trim() +
                district.trim() +
                detailAddress.trim();
        return stringBuilder;
    }
}