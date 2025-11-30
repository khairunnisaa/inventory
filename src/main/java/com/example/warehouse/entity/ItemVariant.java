package com.example.warehouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_variants",
       uniqueConstraints = @UniqueConstraint(name = "uk_variant_sku", columnNames = "sku"),
       indexes = {
           @Index(name = "idx_variant_item", columnList = "item_id"),
           @Index(name = "idx_variant_sku", columnList = "sku")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Item is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_variant_item"))
    private Item item;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @NotBlank(message = "Variant name is required")
    @Size(max = 255, message = "Variant name must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String name; // e.g. "Red - M"

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}

