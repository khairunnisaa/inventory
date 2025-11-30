package com.example.warehouse.controller;

import com.example.warehouse.dto.CreateOrderRequest;
import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.entity.Order;
import com.example.warehouse.entity.OrderLine;
import com.example.warehouse.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order testOrder;
    private CreateOrderRequest testRequest;

    @BeforeEach
    void setUp() {
        Item item = Item.builder()
                .id(1L)
                .name("T-Shirt")
                .basePrice(new BigDecimal("100000"))
                .stockQuantity(10)
                .hasVariants(false)
                .build();

        OrderLine orderLine = OrderLine.builder()
                .id(1L)
                .item(item)
                .quantity(2)
                .unitPrice(new BigDecimal("100000"))
                .lineTotal(new BigDecimal("200000"))
                .build();

        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORDER-123")
                .createdAt(Instant.now())
                .totalAmount(new BigDecimal("200000"))
                .lines(Arrays.asList(orderLine))
                .build();

        testRequest = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .quantity(2)
                                .build()
                ))
                .build();
    }

    @Test
    void createOrder_WithValidRequest_ShouldReturnCreatedOrder() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderNumber").value("ORDER-123"))
                .andExpect(jsonPath("$.totalAmount").value(200000))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines[0].quantity").value(2));
    }

    @Test
    void createOrder_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(null) // Invalid: null itemId
                                .quantity(2)
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_WithEmptyLines_ShouldReturnBadRequest() throws Exception {
        // Given
        CreateOrderRequest emptyRequest = CreateOrderRequest.builder()
                .lines(Arrays.asList())
                .build();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());
    }
}

