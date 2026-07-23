package com.example.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
    Spring Boot启动主类

    @SpringBootApplication = SpringBoot启动主类
    @EnableScheduling 启动定时任务
 */

@SpringBootApplication
@EnableScheduling
public class EcommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class,args);
    }
}
