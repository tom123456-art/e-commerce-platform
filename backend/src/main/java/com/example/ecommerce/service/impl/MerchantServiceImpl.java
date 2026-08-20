package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.MerchantDashboardResponse;
import com.example.ecommerce.dto.MerchantRegisterRequest;
import com.example.ecommerce.dto.ReviewReplyRequest;
import com.example.ecommerce.dto.StoreRequest;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.Store;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.mapper.ReviewMapper;
import com.example.ecommerce.mapper.StoreMapper;
import com.example.ecommerce.service.MerchantService;
import com.example.ecommerce.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MerchantServiceImpl implements MerchantService {
    private static final Logger log = LoggerFactory.getLogger(MerchantServiceImpl.class);

    private final UserService userService;
    private final StoreMapper storeMapper;
    private final ReviewMapper reviewMapper;
    private final ProductMapper productMapper;

    public MerchantServiceImpl(UserService userService, StoreMapper storeMapper, ReviewMapper reviewMapper, ProductMapper productMapper) {
        this.userService = userService;
        this.storeMapper = storeMapper;
        this.reviewMapper = reviewMapper;
        this.productMapper = productMapper;
    }

    /**
     * @param request
     * @return
     */
    @Override
    @Transactional
    public User register(MerchantRegisterRequest request) {
        // 参数校验
        if (request == null) throw new BusinessException(Result.BAD_REQUEST_CODE, "注册请求参数为空");
        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword()))
            throw new BusinessException(Result.BAD_REQUEST_CODE, "两次输入的密码不一致");
        if (userService.getUserByUsername(request.getUsername()) != null)
            throw new BusinessException(Result.CONFLICT_CODE, "用户名已存在");
        // 创建商户用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole("MERCHANT");
        user.setStatus(1);
        // 保存用户
        userService.save(user);
        // 查询拿到刚刚创建的用户，自动创建店铺
        User savedUser = userService.getUserByUsername(user.getUsername());
        Store store = new Store();
        store.setMerchantId(savedUser.getId());
        store.setStoreName(request.getStoreName().trim());
        store.setStoreDescription(request.getStoreDescription().trim());
        store.setContactPhone(request.getPhone().trim());
        store.setContactEmail(request.getEmail().trim());
        store.setStatus(1);
        storeMapper.insert(store);
        return savedUser;
    }

    /**
     * @param merchantId
     * @return
     */
    @Override
    public Store getStore(Long merchantId) {
        return storeMapper.selectByMerchantId(merchantId);
    }

    /**
     * @param merchantId
     * @param request
     * @return
     */
    @Override
    public Store updateStore(Long merchantId, StoreRequest request) {
        // 获取当前商家的店铺信息
        Store store = storeMapper.selectByMerchantId(merchantId);
        if (store == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "店铺不存在");
        store.setStoreName(request.getStoreName());
        store.setStoreDescription(request.getStoreDescription());
        store.setStoreLogo(request.getStoreLogo());
        store.setContactPhone(request.getContactPhone());
        store.setContactEmail(request.getContactEmail());
        store.setAddress(request.getAddress());
        storeMapper.update(store);
        return store;
    }

    /**
     * @param merchantId
     * @return
     */
    @Override
    public List<Review> getReviews(Long merchantId) {
        return reviewMapper.selectByMerchantProducts(merchantId);
    }

    /**
     * @param merchantId
     * @param request
     * @return
     */
    @Override
    @Transactional
    public void replyReviews(Long merchantId, ReviewReplyRequest request) {
        // 获取回复的评论
        Review review = reviewMapper.selectById(request.getReviewId());
        if (review == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "评论不存在");
        // 校验：评论所属的商品是否属于当前商家
        Product product = productMapper.selectById(review.getProductId());
        if (product == null || !product.getMerchantId().equals(merchantId))
            throw new BusinessException(Result.FORBIDDEN_CODE, "评论所属商品不存在");
        reviewMapper.updateReply(request.getReviewId(), request.getReply());
        log.info("回复评论成功，评论ID：{}, 商家ID：{}", request.getReviewId(), merchantId);
    }

    /**
     * @param merchantId
     * @param reviewId
     */
    @Override
    @Transactional
    public void hideReviews(Long merchantId, Long reviewId) {
        // 获取要隐藏的评论
        Review review = reviewMapper.selectById(reviewId);
        if (review == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "评论不存在");
        Product product = productMapper.selectById(review.getProductId());
        if (product == null || !product.getMerchantId().equals(merchantId))
            throw new BusinessException(Result.FORBIDDEN_CODE, "评论所属商品不存在");
        // 软删除
        reviewMapper.updateStatus(reviewId, 0);
        log.info("隐藏评论成功，评论ID：{}, 商家ID：{}", reviewId, merchantId);
    }

    /**
     * @param merchantId
     * @return
     */
    @Override
    public MerchantDashboardResponse getDashboard(Long merchantId) {
        // 构建商家仪表盘数据
        MerchantDashboardResponse response = new MerchantDashboardResponse();
        // 获取全平台的所有商品
        List<Product> products = productMapper.selectAll();
        // 统计当前商家的所有商品
        long productCounts = products.stream().filter(
                p -> p.getMerchantId().equals(merchantId)
        ).count();
        // 统计当前商家的所有在售的商品
        long activeProductCounts = products.stream().filter(
                p -> merchantId.equals(p.getMerchantId()) && p.getStatus() == 1
        ).count();
        // 查询商家的所有评论
        List<Review> reviews = reviewMapper.selectByMerchantProducts(merchantId);
        // 评论总数
        long totalReviews = reviews.size();
        // 待回复
        long pendingReplies = reviews.stream().filter(
                r -> r.getReply() == null || r.getReply().isEmpty()
        ).count();
        // 评分
        double avgRating = reviews.isEmpty() ? 0 : reviews.stream().mapToInt(
                Review::getRating
        ).average().orElse(0);
        response.setTotalProducts(productCounts);
        response.setActiveProducts(activeProductCounts);
        response.setTotalReviews(totalReviews);
        response.setPendingReplies(pendingReplies);
        response.setAverageRating(Math.round(avgRating * 10.0) / 10.0);  // 四舍五入到小数点后一位
        response.setTotalOrders(0);
        response.setTotalRevenue(0.0);
        return response;
    }
}
