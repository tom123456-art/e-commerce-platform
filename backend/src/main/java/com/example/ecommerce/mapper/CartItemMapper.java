package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author waqwb
 * @description 针对表【cart_item】的数据库操作Mapper
 * @createDate 2026-07-30 08:55:58
 * @Entity com.example.ecommerce.entity.CartItem
 */
@Mapper
public interface CartItemMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CartItem record);

    int insertSelective(CartItem record);

    CartItem selectById(@Param("id") Long id);

    /* 根据userId和productId查询购物车项,可以检查商品是否存在于购物车，可以用来加购合并 */
    CartItem selectByUserAndProduct(@Param("userId") Long userId,
                                    @Param("productId") Long productId);

    /* 查询用户购物车列表 */
    List<CartItemResponse> selectCartByUserId(@Param("userId") Long userId);

    /* 更新购物车中商品的数量，增加UserId作为双重条件，防止用户修改其他用户购物车 */
    int updateQuantity(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("quantity") Integer quantity);


    int increaseQuantity(@Param("id") Long id,
                         @Param("userId") Long userId,
                         @Param("quantity") Integer quantity);


    /* 删除购物车指定商品 */
    int deleteByIdAndUserId(@Param("id") Long id,
                            @Param("userId") Long userId);

    /* 清空购物车 */
    int deleteByUserId(@Param("userId") Long userId);
}
