package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.ProductQueryRequest;
import com.example.ecommerce.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品数据访问接口。
 */
@Mapper
public interface ProductMapper {

    /** 根据 ID 查询商品（商品详情页） */
    Product selectById(Long id);

    /** 查询所有商品（不区分上架/下架状态） */
    List<Product> selectAll();

    /** 分页商品查询，支持多条件的诗选和排序 */
    List<Product> selectPage(@Param("request") ProductQueryRequest request);

    /**
     * 统计符合条件的商品总数（用于分页计算总页数）。
     */
    long countPage(@Param("request") ProductQueryRequest request);

    /** 根据分类 ID 查询商品 */
    List<Product> selectByCategoryId(Integer categoryId);

    /** 插入新商品，useGeneratedKeys 回填自增主键 */
    int insert(Product product);

    /** 更新商品信息（全字段更新） */
    int update(Product product);

    /**
     * 扣减商品库存（乐观锁防超卖）。
     */
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 根据 ID 删除商品 */
    int delete(Long id);
}