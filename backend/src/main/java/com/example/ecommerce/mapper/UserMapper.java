package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问接口
 */
@Mapper
public interface UserMapper {
    // 根据用户ID进行查询
    // select * from user where id=#{id}
    User selectById(Long id);
    // 根据用户名查询，用于登录和注册的时候做校验
    User selectByUsername(String username);

    // 查询所有用户
    List<User> selectAll();

    // 统计用户数量
    long countAll();
    // 插入新用户
    int insert(User user);
    // 更新用户信息
    int update(User user);
    // 删除用户
    int delete(Long id);
    // 更新用户密码，单独更新密码
    // 当方法中有多个参数的时候，使用@Param指定名称
    int updatePassword(@Param("id") Long id, @Param("password") String password);

}
