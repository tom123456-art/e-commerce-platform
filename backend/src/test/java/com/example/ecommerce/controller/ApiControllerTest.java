package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.ProductMetricService;
import com.example.ecommerce.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(
        controllers = {
                ProductController.class
        },
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                        "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration" +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
        }
)
class ApiControllerTest {

        @Autowired
        private MockMvc mockMvc;

        // 所有Service用Mock替换，Controller不会真去查询数据库
        @MockitoBean
        private ProductService productService;
        @MockitoBean
        private CartService cartService;

        @MockitoBean
        private ProductMetricService productMetricService;

        /**
         * 商品列表接口允许匿名访问
         */
        @Test
        void productListEndpointIsPublic() throws Exception {
                Product product = new Product();
                product.setId(1L);
                product.setName("Test Product");
                product.setPrice(new BigDecimal("1999.00"));
                when(productService.getAll()).thenReturn(
                        Collections.singletonList(product)
                );
                mockMvc.perform(get("/api/products"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].name")
                                .value("Test Product"));
        }

}