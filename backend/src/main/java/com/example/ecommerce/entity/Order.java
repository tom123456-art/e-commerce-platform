package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @TableName order
 */
@Data
public class Order implements Serializable {
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
    private String orderNo;
    /**
     *
     */
    private BigDecimal totalAmount;
    /**
     *
     */
    private Integer status;
    /**
     *
     */
    private String address;
    /**
     *
     */
    private String phone;
    /**
     *
     */
    private String receiver;
    /**
     *
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;
    /**
     * 前端传递的目标状态，transient表示不参与序列化，也不会被MyBatis映射到数据库列
     * 设计的目的：在确认收货的场景下，前端只需要传递id和targetStatus，
     * 后端校验状态，然后再去更新status字段，也可以避免前端篡改金额等敏感字段
     */
    private transient Integer targetStatus;
}