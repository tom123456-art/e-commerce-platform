package com.example.ecommerce.dto;

/**
 * 购物车结算请求DTO
 *
 */
public class CartCheckoutRequest {
    // 收货地址ID，对应user_address的主键，通过这个id来查询完整地址
    private Long addressId;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}
