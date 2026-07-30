package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.PaymentCallbackLog;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class PaymentCallbackLogMapperTest {

    @Autowired
    private PaymentCallbackLogMapper paymentCallbackLogMapper;

    private PaymentCallbackLog testLog;

    @BeforeEach
    void setUp() {
        testLog = new PaymentCallbackLog();
        testLog.setOrderNo("TEST" + System.currentTimeMillis());
        testLog.setTradeNo("TRD" + System.currentTimeMillis());
        testLog.setTradeStatus("TRADE_SUCCESS");
        testLog.setRawPayload("{\"test\":\"data\"}");
        testLog.setVerified(0);
        testLog.setProcessed(0);
        testLog.setSuccess(0);
    }

    @org.junit.jupiter.api.Test
    void insert() {
        int count = paymentCallbackLogMapper.insert(testLog);
        assertEquals(1, count);
        assertNotNull(testLog.getId());
    }

    @org.junit.jupiter.api.Test
    void updateResult() {
        paymentCallbackLogMapper.insert(testLog);
        testLog.setVerified(1);
        testLog.setProcessed(1);
        testLog.setSuccess(1);
        int count = paymentCallbackLogMapper.updateResult(testLog);
        assertEquals(1, count);
    }
}