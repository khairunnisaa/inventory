package com.example.warehouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_lines",
       indexes = {
           @Index(name = "idx_order_line_order", columnList = "order_id"),
           @Index(name = "idx_order_line_item", columnList = "item_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_line_order"))
    private Order order;

    @NotNull(message = "Item is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_line_item"))
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", foreignKey = @ForeignKey(name = "fk_order_line_variant"))
    private ItemVariant variant; // nullable if sell base item

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @Min(value = 0, message = "Unit price must be non-negative")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "Line total is required")
    @Min(value = 0, message = "Line total must be non-negative")
    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;
}

