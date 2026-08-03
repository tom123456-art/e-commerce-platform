package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.ProductViewEvent;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class ProductViewEventMapperTest {

    @Autowired
    private ProductViewEventMapper productViewEventMapper;

    private ProductViewEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new ProductViewEvent();
        testEvent.setProductId(1L);
        testEvent.setUserId(1L);
        testEvent.setSource("web");
        testEvent.setViewDate(LocalDate.now());
    }

    @org.junit.jupiter.api.Test
    void insert() {
        int count = productViewEventMapper.insert(testEvent);
        assertEquals(1, count);
        assertNotNull(testEvent.getId());
    }
}