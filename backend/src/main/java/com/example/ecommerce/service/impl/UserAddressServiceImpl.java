package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AddressRequest;
import com.example.ecommerce.entity.UserAddress;
import com.example.ecommerce.mapper.UserAddressMapper;
import com.example.ecommerce.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper userAddressMapper;

    public UserAddressServiceImpl(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }


    /**
     * @param userId
     * @return
     */
    @Override
    public List<UserAddress> getByUserId(Long userId) {
        return userAddressMapper.selectByUserId(userId);
    }

    /**
     * @param id
     * @return
     */
    @Override
    public UserAddress getById(Long id) {
        return userAddressMapper.selectById(id);
    }

    /**
     * @param userId
     * @param request
     * @return
     */
    @Override
    @Transactional
    public UserAddress create(Long userId, AddressRequest request) {
        UserAddress userAddress = toEntity(userId, request);
        // 判断地址是否是默认地址，如果没有地址的话，设置第一个地址为默认地址
        if (Boolean.TRUE.equals(request.getIsDefault()) || getDefaultAddress(userId) == null){
            userAddressMapper.clearDefaultByUserId(userId);// 清除旧默认
            userAddress.setIsDefault(1); // 设置为默认地址
        } else {
            userAddress.setIsDefault(0); // 设置为非默认地址
        }
        userAddressMapper.insert(userAddress);
        // 返回完整的地址信息，包含id、时间戳
        return userAddressMapper.selectById(userAddress.getId());
    }

    /**
     * 把DTO转换成Entity，并且要对字符串进行trim操作
     * @param userId
     * @param request
     * @return
     */
    private UserAddress toEntity(Long userId, AddressRequest request){
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        userAddress.setReceiver(request.getReceiver().trim());
        userAddress.setPhone(request.getPhone().trim());
        userAddress.setProvince(request.getProvince().trim());
        userAddress.setCity(request.getCity().trim());
        userAddress.setDistrict(request.getDistrict().trim());
        userAddress.setDetailAddress(request.getDetailAddress().trim());
        return userAddress;
    }

    /**
     * @param userId
     * @param id
     * @param request
     * @return
     */
    @Override
    @Transactional
    public UserAddress update(Long userId, Long id, AddressRequest request) {
        // 校验地址归属
        UserAddress ownedAddress = getOwnedAddress(userId, id);
        UserAddress address = toEntity(userId, request);
        address.setId(ownedAddress.getId());
        if (Boolean.TRUE.equals(request.getIsDefault())){
            // 显示设为默认：先清除其他的默认，再设置当前地址为默认
            userAddressMapper.clearDefaultByUserId(userId);
            address.setIsDefault(1);
        } else if (request.getIsDefault() == null) {
            // 没给传入是否默认，保留原来默认状态
            address.setIsDefault(
                    ownedAddress.getIsDefault() == null ? 0 : ownedAddress.getIsDefault());
        } else {
            // 取消默认
            address.setIsDefault(0);
        }
        userAddressMapper.update(address);
        return userAddressMapper.selectById(id);
    }

    /**
     * @param userId
     * @param id
     */
    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        // 校验地址归属
        UserAddress ownedAddress = getOwnedAddress(userId, id);
        // 删除地址
        userAddressMapper.delete(id, userId);
        // 如果删除的是默认地址，那么会自动将剩余地址的第一个设置为默认地址
        if (ownedAddress.getIsDefault() != null && ownedAddress.getIsDefault() == 1){
            // 获取当前用户的所有地址
            List<UserAddress> userAddresses = userAddressMapper.selectByUserId(userId);
            if (!userAddresses.isEmpty()){
                setDefault(userId, userAddresses.get(0).getId());
            }
        }
    }

    /**
     * @param userId
     * @param id
     */
    @Override
    public void setDefault(Long userId, Long id) {
        UserAddress ownedAddress = getOwnedAddress(userId, id);
        // 清除用户所有的默认标记
        userAddressMapper.clearDefaultByUserId(userId);
        // 设置目标地址为默认地址
        ownedAddress.setIsDefault(1);
        userAddressMapper.update(ownedAddress);
    }

    /**
     * @param userId
     * @param id
     * @return
     */
    @Override
    public UserAddress getOwnedAddress(Long userId, Long id) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !userId.equals(address.getUserId())){
            throw new BusinessException(Result.NOT_FOUND_CODE, "地址不存在");
        }
        return address;
    }

    /**
     * @param userId
     * @return
     */
    @Override
    public UserAddress getDefaultAddress(Long userId) {
        return userAddressMapper.selectDefaultByUserId(userId);
    }
}
