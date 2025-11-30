package com.example.warehouse.service;

import com.example.warehouse.dto.CreateOrderRequest;
import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.exception.InsufficientStockException;
import com.example.warehouse.repository.ItemRepository;
import com.example.warehouse.repository.ItemVariantRepository;
import com.example.warehouse.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceRobustnessTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemVariantRepository variantRepository;

    @InjectMocks
    private OrderService orderService;

    private Item testItem;
    private ItemVariant testVariant;

    @BeforeEach
    void setUp() {
        testItem = Item.builder()
                .id(1L)
                .name("T-Shirt")
                .basePrice(new BigDecimal("100000"))
                .stockQuantity(10)
                .hasVariants(false)
                .version(0L)
                .build();

        testVariant = ItemVariant.builder()
                .id(1L)
                .item(testItem)
                .sku("TSHIRT-BLACK-M")
                .name("Black - M")
                .price(new BigDecimal("110000"))
                .stockQuantity(5)
                .version(0L)
                .build();
    }

    @Test
    void createOrder_WithDuplicateLines_ShouldThrowException() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .quantity(2)
                                .build(),
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .quantity(1)
                                .build()
                ))
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_WithZeroQuantity_ShouldThrowException() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .quantity(0)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_WithNegativeQuantity_ShouldThrowException() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .quantity(-1)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_WithNegativePrice_ShouldThrowException() {
        // Given
        Item itemWithNegativePrice = Item.builder()
                .id(2L)
                .name("Invalid Item")
                .basePrice(new BigDecimal("-100"))
                .stockQuantity(10)
                .hasVariants(false)
                .version(0L)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(2L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(itemRepository.findById(2L)).thenReturn(Optional.of(itemWithNegativePrice));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_WithExactStock_ShouldSucceed() {
        // Given - order exactly all available stock
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .quantity(10) // Exactly all stock
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            var order = invocation.getArgument(0);
            if (order instanceof com.example.warehouse.entity.Order) {
                ((com.example.warehouse.entity.Order) order).setId(1L);
            }
            return order;
        });

        // When
        var result = orderService.createOrder(request);

        // Then
        assertNotNull(result);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void createOrder_WithMultipleItemsOneOutOfStock_ShouldFailEntireOrder() {
        // Given
        Item item2 = Item.builder()
                .id(2L)
                .name("Coffee")
                .basePrice(new BigDecimal("50000"))
                .stockQuantity(0) // Out of stock
                .hasVariants(false)
                .version(0L)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .quantity(5)
                                .build(),
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(2L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));

        // When & Then
        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(request));
        
        // Verify no stock was reduced
        verify(itemRepository, never()).save(any(Item.class));
        verify(orderRepository, never()).save(any());
    }
}

