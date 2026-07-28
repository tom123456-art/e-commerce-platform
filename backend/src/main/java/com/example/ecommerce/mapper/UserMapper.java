package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问接口（UserMapper）。
 *
 * @Mapper：MyBatis 注解，标记此接口为 Mapper，Spring Boot 启动时
 *   会自动扫描并为其生成代理实现类，注入到 Spring 容器中。
 *   也可以在启动类上用 @MapperScan("com.example.ecommerce.mapper") 批量扫描。
 *
 * 面向接口编程：
 *   - Service 层依赖此接口而非实现类，实现解耦
 *   - 测试时可 Mock 此接口，无需连接真实数据库
 *   - 接口只定义"做什么"，XML 定义"怎么做"，职责分离
 *
 * 与 XML 的绑定关系：
 *   - 接口全限定名 com.example.ecommerce.mapper.UserMapper
 *     必须与 UserMapper.xml 的 namespace 属性完全一致
 *   - 接口方法名（如 selectById）必须与 XML 中 SQL 语句的 id 属性一一对应
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户 ID 查询。
     * 对应 SQL：SELECT * FROM user WHERE id = #{id}
     * MyBatis 自动将 ResultSet 通过 resultMap 映射为 User 对象。
     *
     * @param id 用户主键 ID
     * @return User 对象，未找到时返回 null
     */
    User selectById(Long id);

    /**
     * 根据用户名查询（登录和注册校验时使用）。
     * 用户名有唯一约束，最多返回一条记录。
     *
     * @param username 用户名
     * @return User 对象，未找到时返回 null
     */
    User selectByUsername(String username);

    /** 查询所有用户（后台管理用，生产环境应改用分页查询） */
    List<User> selectAll();

    /** 统计用户总数，用于分页计算。COUNT(1) 与 COUNT(*) 效果相同 */
    long countAll();

    /**
     * 插入新用户。
     * useGeneratedKeys="true" keyProperty="id"：
     *   插入后，数据库自动生成的主键值会回填到 user 对象的 id 属性。
     *   这样调用方插入后可以直接用 user.getId() 获取新 ID。
     *
     * @param user 待插入的用户对象
     * @return 受影响行数（成功返回 1）
     */
    int insert(User user);

    /**
     * 更新用户信息（全字段更新）。
     * update_time 字段由 SQL 的 now() 函数自动更新为当前时间。
     *
     * @param user 待更新的用户对象，id 不能为 null
     * @return 受影响行数（成功返回 1，记录不存在返回 0）
     */
    int update(User user);

    /**
     * 单独更新用户密码（部分更新）。
     *
     * @Param 注解的作用：
     *   - 方法只有一个参数时可省略，MyBatis 自动识别
     *   - 方法有多个参数时必须用 @Param 指定名称，
     *     XML 中通过 #{id} 和 #{password} 引用这些参数
     *
     * @param id       用户 ID
     * @param password 新密码（已加密）
     * @return 受影响行数
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 根据 ID 删除用户（物理删除）。
     * 实际项目建议用逻辑删除（设置 status=0）保留数据可追溯性。
     *
     * @param id 用户 ID
     * @return 受影响行数
     */
    int delete(Long id);
}