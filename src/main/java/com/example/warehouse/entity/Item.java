package com.example.warehouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items", 
       uniqueConstraints = @UniqueConstraint(name = "uk_item_name", columnNames = "name"),
       indexes = @Index(name = "idx_item_name", columnList = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @Min(value = 0, message = "Base price must be non-negative")
    @Column(precision = 19, scale = 2)
    private BigDecimal basePrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @NotNull(message = "Has variants flag is required")
    @Column(nullable = false)
    @Builder.Default
    private Boolean hasVariants = false;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemVariant> variants = new ArrayList<>();
}

