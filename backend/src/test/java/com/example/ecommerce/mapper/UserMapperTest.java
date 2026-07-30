package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional //每个测试结束之后自动回滚
class UserMapperTest {
    @Autowired
    private UserMapper userMapper;

    private User testUser;

    @BeforeEach
    void setUp(){
        //清除可能残留的同名数据，保证测试可以重复执行
        User testuserMapper = userMapper.selectByUsername("testuser_mapper");
        if (testuserMapper != null) {
            userMapper.delete(testuserMapper.getId());
        }
        //构造一个测试用户
        testUser = new User();
        testUser.setUsername("testuser_mapper");
        testUser.setPassword("encoded_password");
        testUser.setNickname("测试用户");
        testUser.setEmail("test@123.com");
        testUser.setPhone("12345678901");
        testUser.setRole("user");
        testUser.setStatus(1);
    }

    @Test
    void insertAndSelectById(){
        //插入用户
        int rows = userMapper.insert(testUser);
        assertEquals(1,rows,"插入影响1行");
        assertNotNull(testUser.getId(),"插入后的Id被回填");

        // 根据ID查询
        User user = userMapper.selectById(testUser.getId());
        assertNotNull(user, "根据ID查询到的用户");
        assertEquals("testuser_mapper", user.getUsername());
        assertEquals("测试用户", user.getNickname());

        System.out.println("查询到的用户: " + user);
    }

    @Test
    void selectByUsername(){
        userMapper.insert(testUser);
        User user = userMapper.selectByUsername("testuser_mapper");
        System.out.println("查询到的用户: " + user);
        System.out.println(user.getId() == testUser.getId());
        System.out.println(user.getRole().equals("user"));
    }

    @Test
    void testUpdate(){
        userMapper.insert(testUser);
        Long testUserId = testUser.getId();
        testUser.setNickname("张三");
        testUser.setPassword("123456");
        testUser.setStatus(0);
        int rows = userMapper.update(testUser);
        if (rows > 0){
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

        int rows = userMapper.delete(testUser.getId());
        if (rows > 0){
            System.out.println("用户删除成功");
        } else {
            System.out.println("用户删除失败");
            User user = userMapper.selectById(testUserId);
            System.out.println(user);
        }
    }


}
