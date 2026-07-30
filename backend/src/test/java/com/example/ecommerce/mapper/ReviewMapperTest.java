package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class ReviewMapperTest {
    @Autowired
    private ReviewMapper reviewMapper;

    private Review review;

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setProductId(1L);
        review.setUserId(1L);
        review.setOrderId(1L);
        review.setRating(5);
        review.setContent("This is a test review.");
    }

    @Test
    void selectByProductId() {
        reviewMapper.insertReview(review);
        List<Review> reviews = reviewMapper.selectByProductId(1L);
        for (Review r : reviews)
            System.out.println(r);
    }

    @Test
    void selectByMerchantProducts() {
        reviewMapper.insertReview(review);
        reviewMapper.selectByMerchantProducts(3L).forEach(System.out::println);
    }

    @Test
    void selectById() {
        reviewMapper.insertReview(review);
        Review selectedById = reviewMapper.selectById(review.getId());
        System.out.println(selectedById);
    }

    @Test
    void updateReply() {
        reviewMapper.insertReview(review);
        reviewMapper.updateReply(review.getId(), "Thank");
    }

    @Test
    void updateStatus() {
        reviewMapper.insertReview(review);
        reviewMapper.updateStatus(review.getId(), 0);
    }

    @Test
    void countByProductId() {
        // 使用包装类 Integer 接收，它可以为 null
        Integer count = reviewMapper.countByProductId(1L);

        // 如果 count 为 null，说明没有记录，我们将其视为 0
        int result = (count == null) ? 0 : count;

        System.out.println(result);
    }

    @Test
    void avgRatingByProductId() {
        double d = reviewMapper.avgRatingByProductId(1L);
        System.out.println(d);
    }
}