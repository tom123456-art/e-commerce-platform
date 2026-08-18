package com.example.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiSearchResponse extends AiBaseResponse {

    private List<AiSearchProduct> products;
    private String query;
    private boolean fallback;
    private String provider;

    @Data
    public static class AiSearchProduct {

        private Long id;
        private String name;
        private Integer category;
        private BigDecimal price;
        private String reason;
    }
}
