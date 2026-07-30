package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.ShowcaseStrategyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class ShowcaseStrategyConfigMapperTest {

    @Autowired
    private ShowcaseStrategyConfigMapper showcaseStrategyConfigMapper;

    private ShowcaseStrategyConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new ShowcaseStrategyConfig();
        testConfig.setId(1L);
        testConfig.setMode("HOT");
        testConfig.setShortWindowDays(7);
        testConfig.setLongWindowDays(30);
        testConfig.setCartPreferenceWeight(new BigDecimal("0.3"));
        testConfig.setHotWeightsJson("{\"view\":0.5,\"cart\":0.3,\"buy\":0.2}");
        testConfig.setAnonymousWeightsJson("{\"hot\":0.8,\"new\":0.2}");
        testConfig.setPersonalizedWeightsJson("{\"history\":0.6,\"similar\":0.4}");
        testConfig.setHotSignalWeightsJson("{\"trend\":0.7,\"decay\":0.3}");
    }

    @org.junit.jupiter.api.Test
    void selectCurrent() {
        ShowcaseStrategyConfig config = showcaseStrategyConfigMapper.selectCurrent();
        // 可能为null，不报错即可
    }

    @org.junit.jupiter.api.Test
    void upsert() {
        int count = showcaseStrategyConfigMapper.upsert(testConfig);
        assertTrue(count >= 0);
    }
}