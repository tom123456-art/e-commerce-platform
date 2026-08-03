package com.example.ecommerce.entity;

import java.io.Serializable;
import java.time.LocalDate;
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
    private LocalDate viewDate;

    /**
     * 
     */
    private Date viewedAt;

    private static final long serialVersionUID = 1L;
}