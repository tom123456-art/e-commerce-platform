package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 *
 * @TableName product_view_event
 */
@Data
public class ProductViewEvent implements Serializable {
    private static final long serialVersionUID = 1L;
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
}