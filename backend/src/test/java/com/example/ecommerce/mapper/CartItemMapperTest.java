package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class CartItemMapperTest {
    @Autowired
    private CartItemMapper cartItemMapper;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        cartItem = new CartItem();
        cartItem.setUserId(1L);
        cartItem.setProductId(1L);
        cartItem.setQuantity(3);
    }
    @Test
    void slectById() {
        cartItemMapper.insert(cartItem);
        CartItem cartItem1 = cartItemMapper.selectById(cartItem.getId());
        CartItem cartItem2 = cartItemMapper.selectByUserIdAndProductId(1L, 1L);
        System.out.println(cartItem1.equals(cartItem2));
    }
    @Test
    void selectByUserId() {
        cartItemMapper.insert(cartItem);
        cartItemMapper.selestCartByUserId(1L).forEach(System.out::println);
    }

    @Test
    void updateQuantity() {
        cartItemMapper.insert(cartItem);
        System.out.println("原购物车中商品数量：" + cartItem.getQuantity());
        cartItemMapper.updateQuantity(cartItem.getId(), 1L, 5);
        System.out.println("更新后购物车中商品的数量：" + cartItemMapper.selectById(cartItem.getId()).getQuantity());
    }

    @Test
    void increaseQuantity() {
        cartItemMapper.insert(cartItem);
        System.out.println("原购物车中商品数量：" + cartItem.getQuantity());
        cartItemMapper.increaseQuantity(cartItem.getId(),1L, 10);
        System.out.println("增加后购物车中商品的数量：" + cartItemMapper.selectById(cartItem.getId()).getQuantity());
    }
}