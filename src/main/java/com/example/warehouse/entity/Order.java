package com.example.warehouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders",
       uniqueConstraints = @UniqueConstraint(name = "uk_order_number", columnNames = "order_number"),
       indexes = @Index(name = "idx_order_number", columnList = "order_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Order number is required")
    @Size(max = 100, message = "Order number must not exceed 100 characters")
    @Column(name = "order_number", nullable = false, unique = true, length = 100)
    private String orderNumber;

    @NotNull(message = "Created at timestamp is required")
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull(message = "Total amount is required")
    @Min(value = 0, message = "Total amount must be non-negative")
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();
}

