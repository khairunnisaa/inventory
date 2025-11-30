package com.example.warehouse.service;

import com.example.warehouse.dto.CreateOrderRequest;
import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.entity.Order;
import com.example.warehouse.exception.InsufficientStockException;
import com.example.warehouse.exception.ResourceNotFoundException;
import com.example.warehouse.repository.ItemRepository;
import com.example.warehouse.repository.ItemVariantRepository;
import com.example.warehouse.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class OrderServiceTest {

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
                .description("Basic cotton t-shirt")
                .basePrice(new BigDecimal("100000"))
                .stockQuantity(10)
                .hasVariants(true)
                .build();

        testVariant = ItemVariant.builder()
                .id(1L)
                .item(testItem)
                .sku("TSHIRT-BLACK-M")
                .name("Black - M")
                .price(new BigDecimal("110000"))
                .stockQuantity(5)
                .build();
    }

    @Test
    void createOrder_WithItemWithoutVariant_ShouldCreateOrderAndReduceStock() {
        // Given
        Item itemWithoutVariant = Item.builder()
                .id(2L)
                .name("Coffee Beans")
                .basePrice(new BigDecimal("150000"))
                .stockQuantity(20)
                .hasVariants(false)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(2L)
                                .quantity(3)
                                .build()
                ))
                .build();

        when(itemRepository.findById(2L)).thenReturn(Optional.of(itemWithoutVariant));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        Order result = orderService.createOrder(request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("450000"), result.getTotalAmount());
        assertNotNull(result.getOrderNumber());
        assertEquals(1, result.getLines().size());

        // Verify stock was reduced
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(itemCaptor.capture());
        assertEquals(17, itemCaptor.getValue().getStockQuantity()); // 20 - 3 = 17

        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrder_WithVariant_ShouldCreateOrderAndReduceVariantStock() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .variantId(1L)
                                .quantity(2)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(variantRepository.findById(1L)).thenReturn(Optional.of(testVariant));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        Order result = orderService.createOrder(request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("220000"), result.getTotalAmount()); // 110000 * 2
        assertEquals(1, result.getLines().size());

        // Verify variant stock was reduced
        ArgumentCaptor<ItemVariant> variantCaptor = ArgumentCaptor.forClass(ItemVariant.class);
        verify(variantRepository).save(variantCaptor.capture());
        assertEquals(3, variantCaptor.getValue().getStockQuantity()); // 5 - 2 = 3

        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrder_WithInsufficientStock_ShouldThrowException() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .variantId(1L)
                                .quantity(10) // More than available (5)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(variantRepository.findById(1L)).thenReturn(Optional.of(testVariant));

        // When & Then
        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(request));

        // Verify no stock was reduced
        verify(variantRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_WithItemNotFound_ShouldThrowException() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(999L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_WithVariantNotFound_ShouldThrowException() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .variantId(999L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_WithVariantNotBelongingToItem_ShouldThrowException() {
        // Given
        Item otherItem = Item.builder()
                .id(2L)
                .name("Other Item")
                .build();

        ItemVariant variantOfOtherItem = ItemVariant.builder()
                .id(2L)
                .item(otherItem)
                .sku("OTHER-SKU")
                .name("Other Variant")
                .price(new BigDecimal("50000"))
                .stockQuantity(10)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .variantId(2L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(variantRepository.findById(2L)).thenReturn(Optional.of(variantOfOtherItem));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_WithMultipleLines_ShouldCreateOrderWithAllLines() {
        // Given
        Item itemWithoutVariant = Item.builder()
                .id(2L)
                .name("Coffee Beans")
                .basePrice(new BigDecimal("150000"))
                .stockQuantity(20)
                .hasVariants(false)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .lines(Arrays.asList(
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(1L)
                                .variantId(1L)
                                .quantity(2)
                                .build(),
                        CreateOrderRequest.OrderLineRequest.builder()
                                .itemId(2L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(itemWithoutVariant));
        when(variantRepository.findById(1L)).thenReturn(Optional.of(testVariant));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        Order result = orderService.createOrder(request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("370000"), result.getTotalAmount()); // 220000 + 150000
        assertEquals(2, result.getLines().size());

        // Verify variant stock was reduced (for line with variant)
        // findById is called twice: once during validation, once during stock update
        verify(variantRepository, times(2)).findById(1L);
        verify(variantRepository).save(any(ItemVariant.class));
        
        // Verify item stock was reduced (for line without variant)
        // findById is called twice: once during validation, once during stock update
        verify(itemRepository, times(2)).findById(2L);
        verify(itemRepository).save(any(Item.class));
        
        // Verify order was saved twice (initial save and final save with lines)
        verify(orderRepository, times(2)).save(any(Order.class));
    }
}

