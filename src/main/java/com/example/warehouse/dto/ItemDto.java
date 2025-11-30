package com.example.warehouse.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer stockQuantity;
    private Boolean hasVariants;
    private List<ItemVariantDto> variants;
}

