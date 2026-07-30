package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.ProductQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.example.ecommerce.entity.Product;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional //每个测试结束之后自动回滚
class ProductMapperTest {
    @Autowired
    private ProductMapper productMapper;

    private Product testProduct;

    @BeforeEach
    void setUp(){
        testProduct = new Product();
        testProduct.setName("测试商品1");
        testProduct.setDescription("这是一个测试商品");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(100);
        testProduct.setImage("test.jpg");
        testProduct.setCategoryId(1);
        testProduct.setMerchantId(1L);
        testProduct.setStatus(1);
    }

    @Test
    void selectPage_WithKeyWord(){
        productMapper.insert(testProduct);
        ProductQueryRequest request = new ProductQueryRequest();
        request.setKeyword("测试");
        request.setPage(1);
        request.setPageSize(10);
        List<Product> products = productMapper.selectPage(request);

        for (Product product : products) {
            System.out.println(product.getName());
        }

        System.out.println(
                products.stream().anyMatch(
                        product -> product.getName().contains("测试")));
    }

    @Test
    void selectPage_WithPriceRange(){
        productMapper.insert(testProduct);
        ProductQueryRequest request = new ProductQueryRequest();
        request.setMinPrice(new BigDecimal("50"));
        request.setMaxPrice(new BigDecimal("150"));
        request.setPage(1);
        request.setPageSize(10);
        List<Product> products = productMapper.selectPage(request);
        boolean flag = products.stream().allMatch(
                product ->
                        product.getPrice().compareTo(new BigDecimal("50")) >= 0 &&
                        product.getPrice().compareTo(new BigDecimal("150")) <= 0
        );
        if (flag){
            System.out.println("商品价格范围在50-150之间");
        } else {
          System.out.println("商品价格不在50-150区间");
        }
    }


    @Test
    void selectPage_WithSorting(){
        productMapper.insert(testProduct);
        Product product = new Product();
        product.setName("商品2");
        product.setPrice(new BigDecimal("111.11"));
        product.setStatus(1);
        product.setStock(100);
        product.setCategoryId(1);
        product.setMerchantId(1L);
        productMapper.insert(product);

        ProductQueryRequest request = new ProductQueryRequest();
        request.setPage(1);
        request.setPageSize(10);
        request.setSortBy("price");
        request.setSortDirection("asc");
        List<Product> products = productMapper.selectPage(request);
        if (products.size() >= 2){
            System.out.println(
                    products.get(0).getPrice().compareTo(products.get(1).getPrice()) <= 0
            );
        }
    }

    //乐观锁验证，如果库存重组，扣减成功，返回1
    @Test
    void decreaseStock_Success(){
        productMapper.insert(testProduct);
        Long testProductId = testProduct.getId();
        Integer status = testProduct.getStatus();
        switch (status) {
            case 1:
                int rows = productMapper.decreaseStock(testProductId, 110);
                if (rows > 0) System.out.println("库存充足，扣减成功，返回1");
                else System.out.println("库存不足，扣减失败，返回0");
                Product product = productMapper.selectById(testProductId);
                System.out.println("库存剩余：" + product.getStock());
                break;
            case 0:
                System.out.println("商品状态为下架");
        }
    }
}
