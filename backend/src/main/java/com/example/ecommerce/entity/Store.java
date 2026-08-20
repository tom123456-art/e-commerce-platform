package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @TableName store
 */
@Data
public class Store implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    private Long id;
    /**
     *
     */
    private Long merchantId;
    /**
     *
     */
    private String storeName;
    /**
     *
     */
    private String storeDescription;
    /**
     *
     */
    private String storeLogo;
    /**
     *
     */
    private String contactPhone;
    /**
     *
     */
    private String contactEmail;
    /**
     *
     */
    private String address;
    /**
     *
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
}