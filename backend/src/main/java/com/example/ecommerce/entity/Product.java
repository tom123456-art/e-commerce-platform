package com.example.ecommerce.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName product
 */
@Data
public class Product implements Serializable {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private String description;

    /**
     * 
     */
    private BigDecimal price;

    /**
     * 
     */
    private Integer stock;

    /**
     * 
     */
    private String image;

    /**
     * 
     */
    private Integer categoryId;

    /**
     * 
     */
    private Long merchantId;

    /**
     * 商品状态：1上架 0下架
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