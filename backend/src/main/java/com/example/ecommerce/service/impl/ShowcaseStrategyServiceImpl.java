package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.ShowcaseProperties;
import com.example.ecommerce.config.ShowcaseSchemaSupport;
import com.example.ecommerce.dto.ShowcaseMetricSummary;
import com.example.ecommerce.dto.ShowcaseStrategyRequest;
import com.example.ecommerce.dto.ShowcaseStrategyResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ShowcaseStrategyConfig;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.mapper.ShowcaseStrategyConfigMapper;
import com.example.ecommerce.service.ProductMetricService;
import com.example.ecommerce.service.ShowcaseStrategyService;
import com.example.ecommerce.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * <h2>展示策略服务实现类 - 智能推荐权重的动态调优</h2>
 *
 * <h3>一、类的作用与职责</h3>
 * <p>
 * ShowcaseStrategyServiceImpl 管理首页商品展示的推荐策略：
 * <ul>
 *   <li><strong>策略配置管理</strong>：保存和加载推荐权重配置</li>
 *   <li><strong>手动模式（MANUAL）</strong>：管理员手动设置各维度权重</li>
 *   <li><strong>自动模式（AUTO）</strong>：系统根据业务指标自动调优权重</li>
 *   <li><strong>指标分析</strong>：汇总时间窗口内的业务指标，计算趋势信号</li>
 * </ul>
 * </p>
 *
 * <h3>二、设计模式说明</h3>
 * <p>
 * 1. <strong>策略模式（Strategy Pattern）</strong>：MANUAL 和 AUTO 是两种策略，
 *    根据配置动态选择<br>
 * 2. <strong>指数移动平均（EMA）</strong>：自动调优使用 blend 函数实现新旧权重的平滑过渡<br>
 * 3. <strong>单例配置模式</strong>：整个系统只有一条策略配置记录（ID=1），
 *    使用 upsert 保证原子性
 * </p>
 *
 * <h3>三、自动调优算法说明</h3>
 * <pre>
 * 1. 获取短期（7天）和长期（30天）的业务指标
 * 2. 计算动量信号（短期/长期的比值）
 * 3. 基于动量信号计算目标权重
 * 4. 使用 EMA 平滑过渡：newWeight = old * (1-alpha) + target * alpha
 * 5. 归一化权重（保证同一组权重之和为 1）
 * 6. 持久化新配置并清除展示缓存
 * </pre>
 *
 * <h3>四、权重分组说明</h3>
 * <pre>
 * 热门商品权重（hot）：销量、收入、订单数、新鲜度、库存
 * 匿名推荐权重（anonymous）：热度、新鲜度、库存、价格亲和
 * 个性化推荐权重（personalized）：品类偏好、热度、价格、新鲜度、库存
 * 热度信号权重（hotSignal）：销量、收入、订单数、新鲜度
 * 购物车偏好权重（cartPreferenceWeight）：单独一个 0-1 的值
 * </pre>
 *
 * @author ecommerce-team
 * @see ShowcaseStrategyService 展示策略服务接口定义
 * @see ShowcaseProperties 展示策略配置属性
 * @see ProductMetricService 商品指标服务（提供数据支撑）
 * @since 1.0.0
 */
@Service
public class ShowcaseStrategyServiceImpl implements ShowcaseStrategyService {

    private static final Logger log = LoggerFactory.getLogger(ShowcaseStrategyServiceImpl.class);

    /**
     * 手动模式标识
     */
    private static final String MODE_MANUAL = "MANUAL";
    /**
     * 自动模式标识
     */
    private static final String MODE_AUTO = "AUTO";
    /**
     * 默认短期窗口（天）
     */
    private static final int DEFAULT_SHORT_WINDOW = 7;
    /**
     * 默认长期窗口（天）
     */
    private static final int DEFAULT_LONG_WINDOW = 30;
    /**
     * EMA 平滑系数（0-1，越大越激进）
     */
    private static final double AUTO_BLEND_ALPHA = 0.35D;

    private final ShowcaseSchemaSupport showcaseSchemaSupport;
    private final ShowcaseStrategyConfigMapper showcaseStrategyConfigMapper;
    private final ProductMetricService productMetricService;
    private final ProductMapper productMapper;
    private final ShowcaseProperties showcaseProperties;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final boolean startupInitializationEnabled;

    public ShowcaseStrategyServiceImpl(ShowcaseSchemaSupport showcaseSchemaSupport,
                                       ShowcaseStrategyConfigMapper showcaseStrategyConfigMapper,
                                       ProductMetricService productMetricService,
                                       ProductMapper productMapper,
                                       ShowcaseProperties showcaseProperties,
                                       RedisUtil redisUtil,
                                       ObjectMapper objectMapper,
                                       @Value("${ecommerce.showcase.startup-initialization-enabled:true}") boolean startupInitializationEnabled) {
        this.showcaseSchemaSupport = showcaseSchemaSupport;
        this.showcaseStrategyConfigMapper = showcaseStrategyConfigMapper;
        this.productMetricService = productMetricService;
        this.productMapper = productMapper;
        this.showcaseProperties = showcaseProperties;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.startupInitializationEnabled = startupInitializationEnabled;
    }

    /**
     * <h3>应用启动时初始化 - 加载策略配置到内存</h3>
     *
     * <p><strong>@PostConstruct 说明</strong>：</p>
     * <p>
     * Spring 容器完成 Bean 创建和依赖注入后自动调用。
     * 用于在应用启动时将数据库中的策略配置加载到内存中的 ShowcaseProperties。
     * </p>
     */
    @PostConstruct
    public void initialize() {
        if (!startupInitializationEnabled) {
            return;
        }
        try {
            showcaseSchemaSupport.ensureSchema();
            applyConfigToProperties(loadOrCreateConfig());
        } catch (Exception ex) {
            log.warn("Showcase strategy initialization skipped: {}", ex.getMessage());
        }
    }

    /**
     * <h3>获取当前策略配置</h3>
     *
     * <p><strong>synchronized 说明</strong>：防止并发读写导致配置不一致。</p>
     *
     * @return 策略响应（包含权重配置、指标汇总、趋势数据）
     */
    @Override
    public synchronized ShowcaseStrategyResponse getStrategy() {
        ShowcaseStrategyConfig config = loadOrCreateConfig();
        applyConfigToProperties(config);  // 同步到内存中的 ShowcaseProperties
        return buildResponse(config);
    }

    /**
     * <h3>保存策略配置 - 支持手动和自动两种模式</h3>
     *
     * <p><strong>处理流程</strong>：</p>
     * <ol>
     *   <li>校验窗口天数和模式参数</li>
     *   <li>手动模式：应用用户提交的权重配置</li>
     *   <li>自动模式：保存配置后执行自动调优</li>
     * </ol>
     *
     * @param request 策略请求对象
     * @return 更新后的策略响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized ShowcaseStrategyResponse saveStrategy(ShowcaseStrategyRequest request) {
        ShowcaseStrategyRequest safeRequest = request == null ? new ShowcaseStrategyRequest() : request;
        ShowcaseStrategyConfig config = loadOrCreateConfig();

        // 参数校验
        int shortWindowDays = validateShortWindow(resolveOrDefault(
                safeRequest.getShortWindowDays(), config.getShortWindowDays(), DEFAULT_SHORT_WINDOW));
        int longWindowDays = validateLongWindow(resolveOrDefault(
                safeRequest.getLongWindowDays(), config.getLongWindowDays(), DEFAULT_LONG_WINDOW), shortWindowDays);
        String mode = normalizeMode(safeRequest.getMode(), config.getMode());

        config.setMode(mode);
        config.setShortWindowDays(shortWindowDays);
        config.setLongWindowDays(longWindowDays);

        if (MODE_MANUAL.equals(mode)) {
            // 手动模式：应用用户提交的权重
            applyManualRequest(config, safeRequest);
            return persistAndRespond(config);
        }

        // 自动模式：保存配置后执行一次自动调优
        persistConfig(snapshotCurrentConfig(config));
        return autoTuneInternal(true);
    }

    /**
     * <h3>立即执行自动调优</h3>
     *
     * <p><strong>业务场景</strong>：管理员在后台点击"立即调优"按钮。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized ShowcaseStrategyResponse autoTuneNow() {
        ShowcaseStrategyConfig current = loadOrCreateConfig();
        if (!MODE_AUTO.equalsIgnoreCase(current.getMode())) {
            throw new BusinessException(Result.BAD_REQUEST_CODE,
                    "Automatic tuning is only available in AUTO mode");
        }
        return autoTuneInternal(true);
    }

    /**
     * <h3>自动调优（如果启用）</h3>
     *
     * <p><strong>业务场景</strong>：由定时任务调用，定期自动调优权重。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void autoTuneIfEnabled() {
        try {
            ShowcaseStrategyConfig current = loadOrCreateConfig();
            if (!MODE_AUTO.equalsIgnoreCase(current.getMode())) {
                return;  // 非 AUTO 模式直接返回
            }
            autoTuneInternal(false);
        } catch (Exception ex) {
            log.warn("Failed to auto tune showcase strategy: {}", ex.getMessage());
        }
    }

    /**
     * <h3>自动调优核心算法</h3>
     *
     * <p><strong>算法流程</strong>：</p>
     * <ol>
     *   <li>获取短期和长期的业务指标汇总</li>
     *   <li>如果指标为空（新系统），只更新时间戳</li>
     *   <li>计算动量信号（短期/长期比值）</li>
     *   <li>基于动量信号计算各组权重的目标值</li>
     *   <li>使用 EMA 平滑过渡（alpha=0.35）</li>
     *   <li>归一化权重</li>
     *   <li>持久化并清除缓存</li>
     * </ol>
     */
    private ShowcaseStrategyResponse autoTuneInternal(boolean returnResponse) {
        ShowcaseStrategyConfig config = loadOrCreateConfig();
        applyConfigToProperties(config);

        // 获取短期和长期指标汇总
        ShowcaseMetricSummary shortSummary = productMetricService.summarizeWindow(config.getShortWindowDays());
        ShowcaseMetricSummary longSummary = productMetricService.summarizeWindow(config.getLongWindowDays());

        // 无足够数据时只更新时间戳（新系统冷启动场景）
        if (!hasMeaningfulMetrics(shortSummary, longSummary)) {
            config.setLastAutoTunedAt(new Date());
            persistConfig(snapshotCurrentConfig(config));
            ShowcaseStrategyConfig persisted = reloadConfig();
            applyConfigToProperties(persisted);
            evictShowcaseCache();
            return returnResponse ? buildResponse(persisted) : null;
        }

        // 计算动量信号
        AutoSignals signals = calculateSignals(config, shortSummary, longSummary);

        // 调优 4 组权重
        tuneHotWeights(signals);
        tuneAnonymousWeights(signals);
        tunePersonalizedWeights(signals);
        tuneHotSignalWeights(signals);

        // 调优 cartPreferenceWeight（限定在 [0.30, 0.85]）
        showcaseProperties.setCartPreferenceWeight(clamp(
                blend(showcaseProperties.getCartPreferenceWeight(), targetCartPreference(signals), AUTO_BLEND_ALPHA),
                0.30D, 0.85D
        ));

        // 持久化调优后的配置
        ShowcaseStrategyConfig tuned = snapshotCurrentConfig(config);
        tuned.setMode(MODE_AUTO);
        tuned.setShortWindowDays(config.getShortWindowDays());
        tuned.setLongWindowDays(config.getLongWindowDays());
        tuned.setLastAutoTunedAt(new Date());
        persistConfig(tuned);

        ShowcaseStrategyConfig persisted = reloadConfig();
        applyConfigToProperties(persisted);
        evictShowcaseCache();
        return returnResponse ? buildResponse(persisted) : null;
    }

    // ==================== 手动模式处理 ====================

    private void applyManualRequest(ShowcaseStrategyConfig config, ShowcaseStrategyRequest request) {
        ShowcaseProperties.HotWeights hot = normalizeHotWeights(request.getHot(), showcaseProperties.getHot());
        ShowcaseProperties.AnonymousWeights anonymous = normalizeAnonymousWeights(request.getAnonymous(), showcaseProperties.getAnonymous());
        ShowcaseProperties.PersonalizedWeights personalized = normalizePersonalizedWeights(request.getPersonalized(), showcaseProperties.getPersonalized());
        ShowcaseProperties.HotSignalWeights hotSignal = normalizeHotSignalWeights(request.getHotSignal(), showcaseProperties.getHotSignal());

        showcaseProperties.setHot(hot);
        showcaseProperties.setAnonymous(anonymous);
        showcaseProperties.setPersonalized(personalized);
        showcaseProperties.setHotSignal(hotSignal);
        showcaseProperties.setCartPreferenceWeight(validateCartPreferenceWeight(
                resolveOrDefault(request.getCartPreferenceWeight(),
                        config.getCartPreferenceWeight() == null ? showcaseProperties.getCartPreferenceWeight() : config.getCartPreferenceWeight().doubleValue(),
                        showcaseProperties.getCartPreferenceWeight())
        ));
    }

    private ShowcaseStrategyResponse persistAndRespond(ShowcaseStrategyConfig config) {
        persistConfig(snapshotCurrentConfig(config));
        ShowcaseStrategyConfig persisted = reloadConfig();
        applyConfigToProperties(persisted);
        evictShowcaseCache();
        return buildResponse(persisted);
    }

    // ==================== 响应构建 ====================

    private ShowcaseStrategyResponse buildResponse(ShowcaseStrategyConfig config) {
        ShowcaseStrategyConfig safeConfig = config == null ? loadOrCreateConfig() : config;
        ShowcaseStrategyResponse response = new ShowcaseStrategyResponse();
        response.setMode(normalizeMode(safeConfig.getMode(), MODE_MANUAL));
        response.setShortWindowDays(resolveOrDefault(safeConfig.getShortWindowDays(), DEFAULT_SHORT_WINDOW, DEFAULT_SHORT_WINDOW));
        response.setLongWindowDays(resolveOrDefault(safeConfig.getLongWindowDays(), DEFAULT_LONG_WINDOW, DEFAULT_LONG_WINDOW));
        response.setCartPreferenceWeight(showcaseProperties.getCartPreferenceWeight());
        response.setHot(copy(showcaseProperties.getHot(), ShowcaseProperties.HotWeights.class));
        response.setAnonymous(copy(showcaseProperties.getAnonymous(), ShowcaseProperties.AnonymousWeights.class));
        response.setPersonalized(copy(showcaseProperties.getPersonalized(), ShowcaseProperties.PersonalizedWeights.class));
        response.setHotSignal(copy(showcaseProperties.getHotSignal(), ShowcaseProperties.HotSignalWeights.class));
        response.setLastAutoTunedAt(safeConfig.getLastAutoTunedAt());
        response.setUpdateTime(safeConfig.getUpdateTime());
        response.setShortWindowSummary(productMetricService.summarizeWindow(response.getShortWindowDays()));
        response.setLongWindowSummary(productMetricService.summarizeWindow(response.getLongWindowDays()));
        response.setRecentDailyMetrics(productMetricService.recentDailyMetrics(14));
        return response;
    }

    // ==================== 权重调优方法 ====================

    /**
     * 调优热门商品权重
     * <p>
     * 基于动量信号调整各维度权重：
     * - 销量动量高 → 提高销量权重
     * - 收入动量高 → 提高收入权重
     * - 库存健康 → 提高库存权重
     * </p>
     */
    private void tuneHotWeights(AutoSignals signals) {
        double[] tuned = blendAndNormalize(
                new double[]{
                        showcaseProperties.getHot().getSales(),
                        showcaseProperties.getHot().getRevenue(),
                        showcaseProperties.getHot().getOrders(),
                        showcaseProperties.getHot().getFreshness(),
                        showcaseProperties.getHot().getInventory()
                },
                new double[]{
                        0.35D + 0.30D * signals.salesMomentum + 0.08D * signals.cartMomentum,
                        0.18D + 0.22D * signals.revenueMomentum + 0.08D * signals.paymentRate,
                        0.18D + 0.22D * signals.orderMomentum + 0.08D * signals.cartPaymentRate,
                        0.12D + 0.22D * signals.viewMomentum,
                        0.08D + 0.20D * signals.inventoryHealth
                }
        );
        ShowcaseProperties.HotWeights hot = new ShowcaseProperties.HotWeights();
        hot.setSales(tuned[0]);
        hot.setRevenue(tuned[1]);
        hot.setOrders(tuned[2]);
        hot.setFreshness(tuned[3]);
        hot.setInventory(tuned[4]);
        showcaseProperties.setHot(hot);
    }

    private void tuneAnonymousWeights(AutoSignals signals) {
        double[] tuned = blendAndNormalize(
                new double[]{
                        showcaseProperties.getAnonymous().getHot(),
                        showcaseProperties.getAnonymous().getFreshness(),
                        showcaseProperties.getAnonymous().getInventory(),
                        showcaseProperties.getAnonymous().getAffordability()
                },
                new double[]{
                        0.35D + 0.22D * signals.viewMomentum + 0.18D * signals.salesMomentum,
                        0.20D + 0.24D * signals.viewMomentum,
                        0.15D + 0.20D * signals.inventoryHealth,
                        0.10D + 0.18D * (1D - signals.paymentRate) + 0.10D * (1D - signals.cartPaymentRate)
                }
        );
        ShowcaseProperties.AnonymousWeights anonymous = new ShowcaseProperties.AnonymousWeights();
        anonymous.setHot(tuned[0]);
        anonymous.setFreshness(tuned[1]);
        anonymous.setInventory(tuned[2]);
        anonymous.setAffordability(tuned[3]);
        showcaseProperties.setAnonymous(anonymous);
    }

    private void tunePersonalizedWeights(AutoSignals signals) {
        double[] tuned = blendAndNormalize(
                new double[]{
                        showcaseProperties.getPersonalized().getCategory(),
                        showcaseProperties.getPersonalized().getHot(),
                        showcaseProperties.getPersonalized().getPrice(),
                        showcaseProperties.getPersonalized().getFreshness(),
                        showcaseProperties.getPersonalized().getInventory()
                },
                new double[]{
                        0.35D + 0.25D * signals.viewToCartRate + 0.10D * signals.cartMomentum,
                        0.20D + 0.25D * signals.salesMomentum + 0.10D * signals.viewMomentum,
                        0.10D + 0.18D * (1D - signals.paymentRate),
                        0.10D + 0.18D * signals.viewMomentum,
                        0.05D + 0.18D * signals.inventoryHealth
                }
        );
        ShowcaseProperties.PersonalizedWeights personalized = new ShowcaseProperties.PersonalizedWeights();
        personalized.setCategory(tuned[0]);
        personalized.setHot(tuned[1]);
        personalized.setPrice(tuned[2]);
        personalized.setFreshness(tuned[3]);
        personalized.setInventory(tuned[4]);
        showcaseProperties.setPersonalized(personalized);
    }

    private void tuneHotSignalWeights(AutoSignals signals) {
        double[] tuned = blendAndNormalize(
                new double[]{
                        showcaseProperties.getHotSignal().getSales(),
                        showcaseProperties.getHotSignal().getRevenue(),
                        showcaseProperties.getHotSignal().getOrders(),
                        showcaseProperties.getHotSignal().getFreshness()
                },
                new double[]{
                        0.35D + 0.30D * signals.salesMomentum,
                        0.20D + 0.22D * signals.revenueMomentum + 0.06D * signals.paymentRate,
                        0.20D + 0.24D * signals.orderMomentum + 0.06D * signals.cartPaymentRate,
                        0.10D + 0.24D * signals.viewMomentum
                }
        );
        ShowcaseProperties.HotSignalWeights hotSignal = new ShowcaseProperties.HotSignalWeights();
        hotSignal.setSales(tuned[0]);
        hotSignal.setRevenue(tuned[1]);
        hotSignal.setOrders(tuned[2]);
        hotSignal.setFreshness(tuned[3]);
        showcaseProperties.setHotSignal(hotSignal);
    }

    private double targetCartPreference(AutoSignals signals) {
        return clamp(
                0.35D + 0.20D * signals.viewToCartRate + 0.25D * signals.cartPaymentRate + 0.10D * signals.salesMomentum,
                0.30D, 0.85D
        );
    }

    // ==================== 信号计算 ====================

    private AutoSignals calculateSignals(ShowcaseStrategyConfig config,
                                         ShowcaseMetricSummary shortSummary,
                                         ShowcaseMetricSummary longSummary) {
        AutoSignals signals = new AutoSignals();
        // 计算每日平均值
        double shortViewsPerDay = perDay(shortSummary.getViewCount(), config.getShortWindowDays());
        double longViewsPerDay = perDay(longSummary.getViewCount(), config.getLongWindowDays());
        double shortCartPerDay = perDay(shortSummary.getCartAddCount(), config.getShortWindowDays());
        double longCartPerDay = perDay(longSummary.getCartAddCount(), config.getLongWindowDays());
        double shortOrdersPerDay = perDay(shortSummary.getPaidOrderCount(), config.getShortWindowDays());
        double longOrdersPerDay = perDay(longSummary.getPaidOrderCount(), config.getLongWindowDays());
        double shortSalesPerDay = perDay(shortSummary.getPaidQuantity(), config.getShortWindowDays());
        double longSalesPerDay = perDay(longSummary.getPaidQuantity(), config.getLongWindowDays());
        double shortRevenuePerDay = perDay(shortSummary.getPaidAmount(), config.getShortWindowDays());
        double longRevenuePerDay = perDay(longSummary.getPaidAmount(), config.getLongWindowDays());

        // 计算动量信号（短期/长期比值）
        signals.viewMomentum = momentumScore(shortViewsPerDay, longViewsPerDay);
        signals.cartMomentum = momentumScore(shortCartPerDay, longCartPerDay);
        signals.orderMomentum = momentumScore(shortOrdersPerDay, longOrdersPerDay);
        signals.salesMomentum = momentumScore(shortSalesPerDay, longSalesPerDay);
        signals.revenueMomentum = momentumScore(shortRevenuePerDay, longRevenuePerDay);
        signals.viewToCartRate = clamp(safeDouble(shortSummary.getViewToCartRate()), 0D, 1D);
        signals.paymentRate = clamp(safeDouble(shortSummary.getPaymentRate()), 0D, 1D);
        signals.cartPaymentRate = clamp(safeDouble(shortSummary.getCartPaymentRate()), 0D, 1D);
        signals.inventoryHealth = computeInventoryHealth();
        return signals;
    }

    /**
     * 计算库存健康度
     * <p>
     * 健康度 = 1 - (低库存商品数 / 活跃商品总数)
     * 低库存定义：库存 <= 10
     * </p>
     */
    private double computeInventoryHealth() {
        List<Product> products = productMapper.selectAll();
        if (products == null || products.isEmpty()) {
            return 0.60D;
        }
        long activeCount = 0L;
        long lowStockCount = 0L;
        for (Product product : products) {
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                continue;
            }
            activeCount++;
            if (product.getStock() != null && product.getStock() <= 10) {
                lowStockCount++;
            }
        }
        if (activeCount == 0L) {
            return 0.60D;
        }
        return clamp(1D - (double) lowStockCount / (double) activeCount, 0.15D, 0.95D);
    }

    private boolean hasMeaningfulMetrics(ShowcaseMetricSummary shortSummary, ShowcaseMetricSummary longSummary) {
        return totalSignal(shortSummary) > 0L || totalSignal(longSummary) > 0L;
    }

    private long totalSignal(ShowcaseMetricSummary summary) {
        if (summary == null) {
            return 0L;
        }
        return safeLong(summary.getViewCount()) + safeLong(summary.getCartAddCount())
                + safeLong(summary.getPaidOrderCount()) + safeLong(summary.getPaidQuantity());
    }

    // ==================== 配置管理 ====================

    private ShowcaseStrategyConfig snapshotCurrentConfig(ShowcaseStrategyConfig base) {
        ShowcaseStrategyConfig snapshot = new ShowcaseStrategyConfig();
        snapshot.setId(base.getId() == null ? 1L : base.getId());
        snapshot.setMode(normalizeMode(base.getMode(), MODE_MANUAL));
        snapshot.setShortWindowDays(resolveOrDefault(base.getShortWindowDays(), DEFAULT_SHORT_WINDOW, DEFAULT_SHORT_WINDOW));
        snapshot.setLongWindowDays(resolveOrDefault(base.getLongWindowDays(), DEFAULT_LONG_WINDOW, DEFAULT_LONG_WINDOW));
        snapshot.setCartPreferenceWeight(BigDecimal.valueOf(showcaseProperties.getCartPreferenceWeight()));
        snapshot.setHotWeightsJson(writeJson(showcaseProperties.getHot()));
        snapshot.setAnonymousWeightsJson(writeJson(showcaseProperties.getAnonymous()));
        snapshot.setPersonalizedWeightsJson(writeJson(showcaseProperties.getPersonalized()));
        snapshot.setHotSignalWeightsJson(writeJson(showcaseProperties.getHotSignal()));
        snapshot.setLastAutoTunedAt(base.getLastAutoTunedAt());
        return snapshot;
    }

    private ShowcaseStrategyConfig loadOrCreateConfig() {
        showcaseSchemaSupport.ensureSchema();
        ShowcaseStrategyConfig config = showcaseStrategyConfigMapper.selectCurrent();
        if (config != null) {
            return config;
        }
        // 首次使用时创建默认配置
        ShowcaseStrategyConfig fallback = new ShowcaseStrategyConfig();
        fallback.setId(1L);
        fallback.setMode(MODE_MANUAL);
        fallback.setShortWindowDays(DEFAULT_SHORT_WINDOW);
        fallback.setLongWindowDays(DEFAULT_LONG_WINDOW);
        fallback.setCartPreferenceWeight(BigDecimal.valueOf(showcaseProperties.getCartPreferenceWeight()));
        fallback.setHotWeightsJson(writeJson(showcaseProperties.getHot()));
        fallback.setAnonymousWeightsJson(writeJson(showcaseProperties.getAnonymous()));
        fallback.setPersonalizedWeightsJson(writeJson(showcaseProperties.getPersonalized()));
        fallback.setHotSignalWeightsJson(writeJson(showcaseProperties.getHotSignal()));
        persistConfig(fallback);
        return reloadConfig();
    }

    private ShowcaseStrategyConfig reloadConfig() {
        ShowcaseStrategyConfig config = showcaseStrategyConfigMapper.selectCurrent();
        if (config == null) {
            throw new BusinessException(Result.ERROR_CODE, "Showcase strategy configuration is not initialized");
        }
        return config;
    }

    private void persistConfig(ShowcaseStrategyConfig config) {
        showcaseStrategyConfigMapper.upsert(config);
    }

    private void applyConfigToProperties(ShowcaseStrategyConfig config) {
        if (config == null) {
            return;
        }
        showcaseProperties.setHot(normalizeHotWeights(readJson(config.getHotWeightsJson(), ShowcaseProperties.HotWeights.class), showcaseProperties.getHot()));
        showcaseProperties.setAnonymous(normalizeAnonymousWeights(readJson(config.getAnonymousWeightsJson(), ShowcaseProperties.AnonymousWeights.class), showcaseProperties.getAnonymous()));
        showcaseProperties.setPersonalized(normalizePersonalizedWeights(readJson(config.getPersonalizedWeightsJson(), ShowcaseProperties.PersonalizedWeights.class), showcaseProperties.getPersonalized()));
        showcaseProperties.setHotSignal(normalizeHotSignalWeights(readJson(config.getHotSignalWeightsJson(), ShowcaseProperties.HotSignalWeights.class), showcaseProperties.getHotSignal()));
        showcaseProperties.setCartPreferenceWeight(validateCartPreferenceWeight(
                config.getCartPreferenceWeight() == null ? showcaseProperties.getCartPreferenceWeight() : config.getCartPreferenceWeight().doubleValue()
        ));
    }

    // ==================== 权重归一化 ====================

    private ShowcaseProperties.HotWeights normalizeHotWeights(ShowcaseProperties.HotWeights raw, ShowcaseProperties.HotWeights fallback) {
        ShowcaseProperties.HotWeights safe = raw == null ? copy(fallback, ShowcaseProperties.HotWeights.class) : raw;
        double[] normalized = normalizeGroup(new double[]{safe.getSales(), safe.getRevenue(), safe.getOrders(), safe.getFreshness(), safe.getInventory()}, "hot");
        ShowcaseProperties.HotWeights weights = new ShowcaseProperties.HotWeights();
        weights.setSales(normalized[0]);
        weights.setRevenue(normalized[1]);
        weights.setOrders(normalized[2]);
        weights.setFreshness(normalized[3]);
        weights.setInventory(normalized[4]);
        return weights;
    }

    private ShowcaseProperties.AnonymousWeights normalizeAnonymousWeights(ShowcaseProperties.AnonymousWeights raw, ShowcaseProperties.AnonymousWeights fallback) {
        ShowcaseProperties.AnonymousWeights safe = raw == null ? copy(fallback, ShowcaseProperties.AnonymousWeights.class) : raw;
        double[] normalized = normalizeGroup(new double[]{safe.getHot(), safe.getFreshness(), safe.getInventory(), safe.getAffordability()}, "anonymous");
        ShowcaseProperties.AnonymousWeights weights = new ShowcaseProperties.AnonymousWeights();
        weights.setHot(normalized[0]);
        weights.setFreshness(normalized[1]);
        weights.setInventory(normalized[2]);
        weights.setAffordability(normalized[3]);
        return weights;
    }

    private ShowcaseProperties.PersonalizedWeights normalizePersonalizedWeights(ShowcaseProperties.PersonalizedWeights raw, ShowcaseProperties.PersonalizedWeights fallback) {
        ShowcaseProperties.PersonalizedWeights safe = raw == null ? copy(fallback, ShowcaseProperties.PersonalizedWeights.class) : raw;
        double[] normalized = normalizeGroup(new double[]{safe.getCategory(), safe.getHot(), safe.getPrice(), safe.getFreshness(), safe.getInventory()}, "personalized");
        ShowcaseProperties.PersonalizedWeights weights = new ShowcaseProperties.PersonalizedWeights();
        weights.setCategory(normalized[0]);
        weights.setHot(normalized[1]);
        weights.setPrice(normalized[2]);
        weights.setFreshness(normalized[3]);
        weights.setInventory(normalized[4]);
        return weights;
    }

    private ShowcaseProperties.HotSignalWeights normalizeHotSignalWeights(ShowcaseProperties.HotSignalWeights raw, ShowcaseProperties.HotSignalWeights fallback) {
        ShowcaseProperties.HotSignalWeights safe = raw == null ? copy(fallback, ShowcaseProperties.HotSignalWeights.class) : raw;
        double[] normalized = normalizeGroup(new double[]{safe.getSales(), safe.getRevenue(), safe.getOrders(), safe.getFreshness()}, "hot signal");
        ShowcaseProperties.HotSignalWeights weights = new ShowcaseProperties.HotSignalWeights();
        weights.setSales(normalized[0]);
        weights.setRevenue(normalized[1]);
        weights.setOrders(normalized[2]);
        weights.setFreshness(normalized[3]);
        return weights;
    }

    /**
     * <h3>权重归一化 - 保证同一组权重之和为 1</h3>
     *
     * <p><strong>归一化公式</strong>：normalized[i] = raw[i] / sum(raw)</p>
     *
     * <p><strong>校验规则</strong>：</p>
     * <ul>
     *   <li>权重值不能为 NaN、Infinity 或负数</li>
     *   <li>至少有一个正数值</li>
     * </ul>
     */
    private double[] normalizeGroup(double[] raw, String label) {
        double sum = 0D;
        double[] sanitized = new double[raw.length];
        for (int index = 0; index < raw.length; index++) {
            double value = raw[index];
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0D) {
                throw new BusinessException(Result.BAD_REQUEST_CODE, "Invalid " + label + " weight value");
            }
            sanitized[index] = value;
            sum += value;
        }
        if (sum <= 0D) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, label + " weights must contain at least one positive value");
        }
        for (int index = 0; index < sanitized.length; index++) {
            sanitized[index] = sanitized[index] / sum;
        }
        return sanitized;
    }

    /**
     * EMA 混合后归一化
     */
    private double[] blendAndNormalize(double[] current, double[] target) {
        double[] blended = new double[current.length];
        for (int index = 0; index < current.length; index++) {
            blended[index] = blend(current[index], target[index], AUTO_BLEND_ALPHA);
        }
        return normalizeGroup(blended, "auto");
    }

    // ==================== 工具方法 ====================

    private <T> T readJson(String json, Class<T> type) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new BusinessException(Result.ERROR_CODE, "Failed to parse showcase strategy configuration");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException(Result.ERROR_CODE, "Failed to serialize showcase strategy configuration");
        }
    }

    private <T> T copy(Object source, Class<T> type) {
        return objectMapper.convertValue(source, type);
    }

    private void evictShowcaseCache() {
        try {
            redisUtil.deleteByPattern("products:showcase:*");
        } catch (Exception ex) {
            log.warn("Failed to evict showcase cache: {}", ex.getMessage());
        }
    }

    private int validateShortWindow(int value) {
        if (value < 1 || value > 30) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "Short window days must be between 1 and 30");
        }
        return value;
    }

    private int validateLongWindow(int value, int shortWindow) {
        if (value < shortWindow || value > 180) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "Long window days must be between short window days and 180");
        }
        return value;
    }

    private double validateCartPreferenceWeight(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0D || value > 1D) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "Cart preference weight must be between 0 and 1");
        }
        return value;
    }

    private String normalizeMode(String requestedMode, String fallbackMode) {
        String safe = requestedMode;
        if (safe == null || safe.trim().isEmpty()) {
            safe = fallbackMode;
        }
        if (safe == null || safe.trim().isEmpty()) {
            safe = MODE_MANUAL;
        }
        String normalized = safe.trim().toUpperCase();
        if (!MODE_MANUAL.equals(normalized) && !MODE_AUTO.equals(normalized)) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "Mode must be MANUAL or AUTO");
        }
        return normalized;
    }

    private int resolveOrDefault(Integer requested, Integer current, int fallback) {
        if (requested != null) return requested;
        if (current != null) return current;
        return fallback;
    }

    private double resolveOrDefault(Double requested, Double current, double fallback) {
        if (requested != null) return requested;
        if (current != null) return current;
        return fallback;
    }

    /**
     * EMA 混合：result = current * (1-alpha) + target * alpha
     */
    private double blend(double current, double target, double alpha) {
        return current * (1D - alpha) + target * alpha;
    }

    /**
     * 动量分数：短期/长期比值，范围 [0, 1]
     */
    private double momentumScore(double shortPerDay, double longPerDay) {
        if (shortPerDay <= 0D && longPerDay <= 0D) {
            return 0.50D;
        }
        if (longPerDay <= 0D) {
            return 1D;
        }
        double ratio = shortPerDay / longPerDay;
        return clamp(ratio, 0.50D, 1.50D) - 0.50D;
    }

    private double perDay(Long total, int days) {
        return days <= 0 ? 0D : (double) safeLong(total) / (double) days;
    }

    private double perDay(BigDecimal total, int days) {
        return days <= 0 || total == null ? 0D : total.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP).doubleValue();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0D : value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 自动调优的信号数据结构
     */
    private static class AutoSignals {
        private double viewMomentum;       // 浏览动量
        private double cartMomentum;       // 加购动量
        private double orderMomentum;      // 订单动量
        private double salesMomentum;      // 销量动量
        private double revenueMomentum;    // 收入动量
        private double viewToCartRate;     // 浏览->加购转化率
        private double paymentRate;        // 浏览->支付转化率
        private double cartPaymentRate;    // 加购->支付转化率
        private double inventoryHealth;    // 库存健康度
    }
}
