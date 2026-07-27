package com.example.ecommerce.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName product_view_event
 */
@Data
public class ProductViewEvent implements Serializable {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private Long productId;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private String source;

    /**
     * 
     */
    private Date viewDate;

    /**
     * 
     */
    private Date viewedAt;

    private static final long serialVersionUID = 1L;
}