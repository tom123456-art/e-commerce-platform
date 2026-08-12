package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserMapper测试类
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test") // 激活application-test.yml，连接ecommerce_test数据库
@Transactional   // 每个测试结束之后自动回滚
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    private User testUser;

    @BeforeEach
    void setUp(){
        // 清除可能残留的同名数据，保证测试可以重复执行
        User testuserMapper = userMapper.selectByUsername("testuser_mapper");
        if(testuserMapper != null){
            userMapper.delete(testuserMapper.getId());
        }
        // 构造一个测试用户
        testUser = new User();
        testUser.setUsername("testuser_mapper");
        testUser.setPassword("encoded_password");
        testUser.setNickname("测试用户");
        testUser.setEmail("test@123.com");
        testUser.setPhone("13112341234");
        testUser.setRole("USER");
        testUser.setStatus(1);
    }

    @Test
    void insertAndSelectById(){
        // 插入用户
        int rows = userMapper.insert(testUser);
        assertEquals(1, rows, "插入应影响1行");

        assertNotNull(testUser.getId(), "插入后的id被回填");

        // 根据ID查询
        User user = userMapper.selectById(testUser.getId());
        assertNotNull(user, "根据ID查询到的用户");
        assertEquals("testuser_mapper", user.getUsername());
        assertEquals("测试用户", user.getNickname());
    }

    @Test
    void selectByUsername(){
        userMapper.insert(testUser);
        User user = userMapper.selectByUsername("testuser_mapper");
        System.out.println("根据用户名查询到用户：" + user);
        System.out.println(user.getId() == testUser.getId());
        System.out.println(user.getRole().equals("USER"));
    }

    @Test
    void testUpdate(){
        userMapper.insert(testUser);
        Long testUserId = testUser.getId();
        testUser.setNickname("张三");
        testUser.setPassword("123123");
        testUser.setStatus(0);
        int rows = userMapper.update(testUser);
        if(rows > 0){
            System.out.println("用户信息更新成功");
            User user = userMapper.selectById(testUserId);
            System.out.println(user);
        } else {
            System.out.println("用户信息更新失败");
        }
    }


    @Test
    void testDelete(){
        userMapper.insert(testUser);
        Long testUserId = testUser.getId();

        int rows = userMapper.delete(testUserId);
        if(rows > 0){
            System.out.println("用户删除成功");
        } else {
            System.out.println("用户删除失败");
            User user = userMapper.selectById(testUserId);
            System.out.println(user);
        }
    }
}