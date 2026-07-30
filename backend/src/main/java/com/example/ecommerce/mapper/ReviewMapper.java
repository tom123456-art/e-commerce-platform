package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 仝佳昀
* @description 针对表【review】的数据库操作Mapper
* @createDate 2026-07-30 14:09:27
* @Entity com.example.ecommerce.entity.Review
*/
@Mapper
public interface ReviewMapper {
    //查询商品的所有评价，status=1是已通过
    List<Review> selectByProductId(Long productId);

    //查询商家所有商品的评价
    List<Review> selectByMerchantProducts(@Param("merchantId") Long merchanId);

    //根据评价的ID查询评价
    Review selectById(Long id);

    //插入评价
    int insertReview(Review review);

    //商家回复
    void updateReply(@Param("id") Long id, @Param("reply") String reply);

    //更新评价状态
    void updateStatus(@Param("id") Long id, @Param("status") int status);

    //统计商品的评价数量
    Integer countByProductId(Long productId);

    //计算商品的平均评分
    double avgRatingByProductId(Long productId);
}




