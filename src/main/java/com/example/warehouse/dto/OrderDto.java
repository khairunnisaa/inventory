package com.example.warehouse.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private Long id;
    private String orderNumber;
    private Instant createdAt;
    private BigDecimal totalAmount;
    private List<OrderLineDto> lines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderLineDto {
        private Long id;
        private Long itemId;
        private String itemName;
        private Long variantId;
        private String variantName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}

