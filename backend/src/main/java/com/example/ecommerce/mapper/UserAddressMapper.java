package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户收货地址数据访问接口。
 *
 * 业务特点：
 *   - 默认地址管理：每个用户只能有一个默认地址，设新默认前需清除旧的
 *   - 安全性校验：更新/删除都带 user_id 条件，防越权
 *   - 排序规则：查询列表时默认地址排最前
 */
@Mapper
public interface UserAddressMapper {

    /** 根据地址 ID 查询 */
    UserAddress selectById(Long id);

    /** 查询用户的所有地址（默认地址排最前） */
    List<UserAddress> selectByUserId(Long userId);

    /** 查询用户的默认地址（下单时自动填充），LIMIT 1 确保只返回一条 */
    UserAddress selectDefaultByUserId(Long userId);

    /** 插入新地址，useGeneratedKeys 回填主键 */
    int insert(UserAddress address);

    /** 更新地址（WHERE 带 user_id 防越权） */
    int update(UserAddress address);

    /** 删除地址（带用户校验） */
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 清除用户的所有默认地址标记（is_default 置 0）。
     * 设置默认地址流程（须在 @Transactional 事务内）：
     *   1. clearDefaultByUserId 清除旧默认
     *   2. update 将目标地址 is_default 设为 1
     */
    int clearDefaultByUserId(Long userId);
}