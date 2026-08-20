package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddressRequest;
import com.example.ecommerce.entity.UserAddress;

import java.util.List;

/**
 * 收货地址服务接口。
 * 调用方：UserAddressController、CartService（结算时解析地址）。
 */
public interface UserAddressService {
    List<UserAddress> getByUserId(Long userId);                              // 查列表

    UserAddress getById(Long id);                                            // 按ID查（不带归属校验）

    UserAddress create(Long userId, AddressRequest request);                 // 新增

    UserAddress update(Long userId, Long id, AddressRequest request);        // 修改

    void delete(Long userId, Long id);                                       // 删除

    void setDefault(Long userId, Long id);                                  // 设默认

    UserAddress getOwnedAddress(Long userId, Long id);                      // 查+归属校验（防越权）

    UserAddress getDefaultAddress(Long userId);                             // 查默认地址
}
