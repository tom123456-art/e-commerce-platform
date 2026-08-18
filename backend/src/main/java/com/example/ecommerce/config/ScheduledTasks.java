package com.example.ecommerce.config;

import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.ShowcaseStrategyService;
import com.example.ecommerce.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// =============================================================================
// 【教学】定时任务配置类
// =============================================================================
//
// 一、什么是定时任务？
// ---------------------------------
// 定时任务是按照预定时间自动执行的任务，常见用途：
//   - 缓存清理：定期清除过期的缓存数据
//   - 数据统计：每天凌晨统计前一天的销售数据
//   - 状态检查：定期检查订单支付状态
//   - 数据同步：定期从外部系统同步数据
//   - 日志清理：定期删除过期的日志文件
//
// 二、@Scheduled 注解详解
// ---------------------------------
// @Scheduled 是 Spring 提供的定时任务注解，支持三种调度方式：
//   1. fixedRate = 5000：每 5 秒执行一次（从上一次开始执行时间点算起）
//   2. fixedDelay = 5000：每 5 秒执行一次（从上一次执行完毕时间点算起）
//   3. cron = "0 0 1 * * ?"：使用 Cron 表达式定义执行时间
//
// 三、Cron 表达式详解（面试常考）
// ---------------------------------
// Cron 表达式格式：秒 分 时 日 月 星期 [年]
//   - 秒：0-59
//   - 分：0-59
//   - 时：0-23
//   - 日：1-31
//   - 月：1-12
//   - 星期：0-7（0 和 7 都表示周日）
//
// 常用示例：
//   "0 0 1 * * ?"     → 每天凌晨 1 点执行
//   "0 0 * * * ?"     → 每小时整点执行
//   "0 * * * * ?"     → 每分钟执行
//   "0 0 0 1 * ?"     → 每月 1 号凌晨执行
//   "0 0 0 * * 1"     → 每周一凌晨执行
//   "0 0/30 * * * ?"  → 每 30 分钟执行
//
// 四、@Component 注解
// ---------------------------------
// 将这个类注册为 Spring Bean。Spring 会自动检测其中的 @Scheduled 方法并调度执行。
// 注意：要使 @Scheduled 生效，还需要在启动类或配置类上添加 @EnableScheduling 注解。
// =============================================================================

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final RedisUtil redisUtil;
    private final ProductService productService;
    private final ShowcaseStrategyService showcaseStrategyService;

    /**
     * 构造器注入依赖。
     *
     * @param redisUtil Redis 工具类（用于缓存操作）
     * @param productService 商品服务（用于缓存预热）
     * @param showcaseStrategyService 展示策略服务（用于自动调优）
     */
    public ScheduledTasks(RedisUtil redisUtil,
                          ProductService productService,
                          ShowcaseStrategyService showcaseStrategyService) {
        this.redisUtil = redisUtil;
        this.productService = productService;
        this.showcaseStrategyService = showcaseStrategyService;
    }

    /**
     * 【教学】每天凌晨 1 点清除商品缓存
     *
     * Cron 表达式 "0 0 1 * * ?" 的含义：
     *   秒=0, 分=0, 时=1, 日=*, 月=*, 星期=?
     *   即每天凌晨 1:00:00 执行
     *
     * 为什么要定期清除缓存？
     *   1. 防止缓存与数据库数据不一致（缓存中的数据可能是旧的）
     *   2. 释放 Redis 内存（避免缓存无限增长）
     *   3. 清除可能的脏数据（如异常中断导致的不完整数据）
     *
     * deleteByPattern("product:*") 使用 Redis 的 SCAN 命令匹配并删除所有
     * 以 "product:" 开头的 Key（如 "product:123"、"product:list:1" 等）。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void clearExpiredCache() {
        redisUtil.deleteByPattern("product:*");
        redisUtil.deleteByPattern("products:*");
        log.info("Product cache cleared");
    }

    /**
     * 【教学】每小时整点执行缓存预热
     *
     * Cron 表达式 "0 0 * * * ?" 的含义：
     *   秒=0, 分=0, 时=*, 日=*, 月=*, 星期=?
     *   即每小时的第 0 分 0 秒执行
     *
     * 缓存预热（Cache Warming）是什么？
     *   在缓存被清除后，第一次请求会"穿透"到数据库（Cache Miss）。
     *   如果此时有大量并发请求，数据库可能承受不住压力。
     *   缓存预热主动将热点数据加载到缓存中，避免冷启动问题。
     *
     * warmUpCache() 方法通常会：
     *   1. 查询热销商品列表
     *   2. 将结果写入 Redis 缓存
     *   3. 后续请求直接从缓存读取
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void updateProductStockStatus() {
        productService.warmUpCache();
        log.info("Product cache warmed up");
    }

    /**
     * 【教学】每分钟检查活跃 Token 数量
     *
     * Cron 表达式 "0 * * * * ?" 的含义：
     *   秒=0, 分=*, 时=*, 日=*, 月=*, 星期=?
     *   即每分钟的第 0 秒执行
     *
     * 这是一个监控任务，记录当前有多少活跃的认证 Token。
     * 在生产环境中，可以帮助发现：
     *   1. Token 泄露（异常多的活跃 Token）
     *   2. 用户未正常登出（Token 未过期）
     *   3. 系统负载情况
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkOrderPaymentStatus() {
        log.info("Active auth token count: {}", redisUtil.keys("auth:token:*").size());
    }

    /**
     * 【教学】每小时 10 分自动调优展示策略
     *
     * Cron 表达式 "0 10 * * * ?" 的含义：
     *   秒=0, 分=10, 时=*, 日=*, 月=*, 星期=?
     *   即每小时的第 10 分钟执行
     *
     * autoTuneIfEnabled() 方法会根据历史数据（点击率、转化率等）
     * 自动调整商品展示策略的权重参数，优化用户体验和转化率。
     * 这是一种简单的"自动化运营"能力。
     */
    @Scheduled(cron = "0 10 * * * ?")
    public void autoTuneShowcaseStrategy() {
        showcaseStrategyService.autoTuneIfEnabled();
        log.info("Showcase strategy auto-tune job finished");
    }
}
