package com.example.ecommerce.mapper;


import com.example.ecommerce.dto.ShowcaseDailyMetricItem;
import com.example.ecommerce.entity.ProductMetricDaily;
import com.example.ecommerce.dto.ShowcaseMetricSummary;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class ProductMetricDailyMapperTest {

    @Autowired
    private ProductMetricDailyMapper productMetricDailyMapper;

    private ProductMetricDaily testMetric;

    @BeforeEach
    void setUp() {
        testMetric = new ProductMetricDaily();
        testMetric.setMetricDate(LocalDate.now());
        testMetric.setProductId(1L);
        testMetric.setViewCount(10);
        testMetric.setCartAddCount(5);
        testMetric.setPaidOrderCount(2);
        testMetric.setPaidQuantity(3);
        testMetric.setPaidAmount(new BigDecimal("99.99"));
    }

    @org.junit.jupiter.api.Test
    void upsertDelta() {
        int count = productMetricDailyMapper.upsertDelta(testMetric);
        assertTrue(count >= 0);
    }

    @org.junit.jupiter.api.Test
    void aggregateSummary() {
        ShowcaseMetricSummary summary = productMetricDailyMapper.aggregateSummary(LocalDate.now().minusDays(7));
        assertNotNull(summary);
    }

    @org.junit.jupiter.api.Test
    void selectRecentDailyTotals() {
        List<ShowcaseDailyMetricItem> items = productMetricDailyMapper.selectRecentDailyTotals(7);
        assertNotNull(items);
    }
}
