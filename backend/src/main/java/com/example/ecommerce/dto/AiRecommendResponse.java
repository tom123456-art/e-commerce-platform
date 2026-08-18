package com.example.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiRecommendResponse extends AiBaseResponse {

    private List<AiRecommendItem> recommendations;
    private String query;
    private boolean fallback;
    private String provider;

    @Data
    public static class AiRecommendItem {

        private Long id;
        private String name;
        private Integer category;
        private BigDecimal price;
        private Integer score;
        private String reason;
    }
}
