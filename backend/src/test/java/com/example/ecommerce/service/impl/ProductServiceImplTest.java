package com.example.ecommerce.service.impl;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.utils.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证Cache Aside缓存模式与缓存失效容错
 */
@ExtendWith(SpringExtension.class)
class ProductServiceImplTest {
    //模拟数据库访问
    @Mock
    private ProductMapper productMapper;
    //模拟缓存访问
    @Mock
    private RedisUtil redisUtil;
    //被测试对象，依赖会倍自动注入
    @InjectMocks
    private ProductServiceImpl productService;

    private  final ObjectMapper objectMapper = new ObjectMapper();

    /*
    * 缓存命中-》直接返回缓存数据，不查数据库
    * */
    @Test
    void getByIdReadsFromCacheBeforeDatabase() throws JsonProcessingException {
        Product product = new Product();
        product.setId(1L);
        product.setName("Mock Product");
        product.setPrice(new BigDecimal("99.9"));
        when(redisUtil.exists("product:1")).thenReturn(true);
        when(redisUtil.get("product:1"))
                .thenReturn(objectMapper.writeValueAsString(product));
        Product result = productService.getById(1L);
        assertEquals("cached", result.getName());
        // 缓存命中的话，绝不能查询数据库
        verify(productMapper, never()).selectById(anyLong());
    }

}