package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.ProductQueryRequest;
import com.example.ecommerce.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品数据访问接口
 */
@Mapper
public interface ProductMapper {
    // 根据ID查看商品
    Product selectById(Long id);

    // 查看所有商品
    List<Product> selectAll();

    // 分页查询商品，支持多条件的筛选和排序
    // @Param("request")是指定参数名为request，可以通过request.keyword等访问属性
    List<Product> selectPage(@Param("request") ProductQueryRequest request);

    // 统计符合条件的商品的数量
    // 可以通过countPage获取总记录数totalCount
    // 总共页数 totalPages = (totalCount + pageSize - 1) / pageSize
    long countPage(@Param("request") ProductQueryRequest request);

    // 根据分类ID查询商品
    List<Product> selectByCategoryId(Integer categoryId);

    // 插入新商品
    int insert(Product product);
    // 更新商品
    int update(Product product);
    // 删除商品
    int delete(Long id);
    // 库存
    // 为了防止超卖
    // update product
    // set stock = stock - #{quantity}
    // where id=#{id} and status=1 and stock >= #{quantity}
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
