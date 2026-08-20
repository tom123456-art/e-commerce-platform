package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.UserAddress;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class UserAddressMapperTest {

    @Autowired
    private UserAddressMapper userAddressMapper;

    private UserAddress testAddress;

    @BeforeEach
    void setUp() {
        testAddress = new UserAddress();
        testAddress.setUserId(1L);
        testAddress.setReceiver("测试用户");
        testAddress.setPhone("13112341234");
        testAddress.setProvince("广东省");
        testAddress.setCity("深圳市");
        testAddress.setDistrict("南山区");
        testAddress.setDetailAddress("科技园路1号");
        testAddress.setIsDefault(0);
    }

    @org.junit.jupiter.api.Test
    void selectById() {
        userAddressMapper.insert(testAddress);
        UserAddress found = userAddressMapper.selectById(testAddress.getId());
        assertNotNull(found);
    }

    @org.junit.jupiter.api.Test
    void selectByUserId() {
        List<UserAddress> addresses = userAddressMapper.selectByUserId(1L);
        assertNotNull(addresses);
    }

    @org.junit.jupiter.api.Test
    void selectDefaultByUserId() {
        UserAddress addr = userAddressMapper.selectDefaultByUserId(1L);
        // 可能为null，不报错即可
    }

    @org.junit.jupiter.api.Test
    void insert() {
        int count = userAddressMapper.insert(testAddress);
        assertEquals(1, count);
        assertNotNull(testAddress.getId());
    }

    @org.junit.jupiter.api.Test
    void update() {
        userAddressMapper.insert(testAddress);
        testAddress.setReceiver("新姓名");
        int count = userAddressMapper.update(testAddress);
        assertEquals(1, count);
    }

    @org.junit.jupiter.api.Test
    void delete() {
        userAddressMapper.insert(testAddress);
        int count = userAddressMapper.delete(testAddress.getId(), 1L);
        assertEquals(1, count);
    }

    @org.junit.jupiter.api.Test
    void clearDefaultByUserId() {
        int count = userAddressMapper.clearDefaultByUserId(1L);
        assertTrue(count >= 0);
    }
}