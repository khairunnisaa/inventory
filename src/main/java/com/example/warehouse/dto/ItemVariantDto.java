package com.example.warehouse.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemVariantDto {

    private Long id;
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long itemId;
}

