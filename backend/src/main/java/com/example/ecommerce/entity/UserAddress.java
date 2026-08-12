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

    public String getFullAddress(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(province.trim());
        stringBuilder.append(city.trim());
        stringBuilder.append(district.trim());
        stringBuilder.append(detailAddress.trim());
        return stringBuilder.toString();
    }

    private static final long serialVersionUID = 1L;
}