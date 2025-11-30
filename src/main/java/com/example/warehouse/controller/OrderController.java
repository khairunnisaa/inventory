package com.example.warehouse.controller;

import com.example.warehouse.dto.CreateOrderRequest;
import com.example.warehouse.dto.OrderDto;
import com.example.warehouse.entity.Order;
import com.example.warehouse.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> getAll() {
        return orderService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public OrderDto getById(@PathVariable Long id) {
        return convertToDto(orderService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return convertToDto(orderService.createOrder(request));
    }

    private OrderDto convertToDto(Order order) {
        List<OrderDto.OrderLineDto> lineDtos = order.getLines().stream()
                .map(line -> OrderDto.OrderLineDto.builder()
                        .id(line.getId())
                        .itemId(line.getItem().getId())
                        .itemName(line.getItem().getName())
                        .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
                        .variantName(line.getVariant() != null ? line.getVariant().getName() : null)
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .lineTotal(line.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .createdAt(order.getCreatedAt())
                .totalAmount(order.getTotalAmount())
                .lines(lineDtos)
                .build();
    }
}

