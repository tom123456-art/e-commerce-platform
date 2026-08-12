package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        review.setContent("This is a great product!");
    }

    @Test
    void selectByProductId(){
        reviewMapper.insertReview(review);
        List<Review> reviews = reviewMapper.selectByProductId(1L);
        for (Review r: reviews)
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
        reviewMapper.updateReply(review.getId(), "Thank you");
    }

    @Test
    void updateStatus() {
        reviewMapper.insertReview(review);
        reviewMapper.updateStatus(review.getId(), 0);
    }

    @Test
    void countByProductId() {
        int i = reviewMapper.countByProductId(1L);
        System.out.println(i);
    }

    @Test
    void avgRatingByProductId() {
        double avgRating = reviewMapper.avgRatingByProductId(1L);
        System.out.println(avgRating);
    }
}