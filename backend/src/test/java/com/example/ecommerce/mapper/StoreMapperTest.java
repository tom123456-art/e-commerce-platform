package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Store;
import org.junit.jupiter.api.BeforeEach;
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
class StoreMapperTest {

    @Autowired
    private StoreMapper storeMapper;

    private Store testStore;

    @BeforeEach
    void setUp() {
        testStore = new Store();
        testStore.setMerchantId(1L);
        testStore.setStoreName("测试店铺");
        testStore.setStoreDescription("这是一家测试店铺");
        testStore.setContactPhone("13112341234");
        testStore.setContactEmail("test@shop.com");
        testStore.setAddress("深圳市南山区");
        testStore.setStatus(1);
    }

    @org.junit.jupiter.api.Test
    void selectByMerchantId() {
        Store store = storeMapper.selectByMerchantId(1L);
        // 可能为null，不报错即可
    }

    @org.junit.jupiter.api.Test
    void insert() {
        storeMapper.insert(testStore);
        assertNotNull(testStore.getId());
    }

    @org.junit.jupiter.api.Test
    void update() {
        storeMapper.insert(testStore);
        testStore.setStoreName("新店铺名");
        storeMapper.update(testStore);
    }
}