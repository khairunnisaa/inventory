package com.example.warehouse.service;

import com.example.warehouse.entity.Item;
import com.example.warehouse.exception.ResourceNotFoundException;
import com.example.warehouse.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;

    @BeforeEach
    void setUp() {
        testItem = Item.builder()
                .id(1L)
                .name("Test Item")
                .description("Test Description")
                .basePrice(new BigDecimal("100000"))
                .stockQuantity(10)
                .hasVariants(false)
                .build();
    }

    @Test
    void findAll_ShouldReturnAllItems() {
        // Given
        List<Item> items = Arrays.asList(testItem);
        when(itemRepository.findAll()).thenReturn(items);

        // When
        List<Item> result = itemService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Item", result.get(0).getName());
        verify(itemRepository).findAll();
    }

    @Test
    void findById_WhenItemExists_ShouldReturnItem() {
        // Given
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // When
        Item result = itemService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        verify(itemRepository).findById(1L);
    }

    @Test
    void findById_WhenItemNotFound_ShouldThrowException() {
        // Given
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> itemService.findById(999L));
        verify(itemRepository).findById(999L);
    }

    @Test
    void create_ShouldSaveAndReturnItem() {
        // Given
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // When
        Item result = itemService.create(testItem);

        // Then
        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        verify(itemRepository).save(testItem);
    }

    @Test
    void update_WhenItemExists_ShouldUpdateAndReturnItem() {
        // Given
        Item updatedItem = Item.builder()
                .name("Updated Item")
                .description("Updated Description")
                .basePrice(new BigDecimal("150000"))
                .stockQuantity(20)
                .hasVariants(false)
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // When
        Item result = itemService.update(1L, updatedItem);

        // Then
        assertNotNull(result);
        verify(itemRepository).findById(1L);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void update_WhenItemNotFound_ShouldThrowException() {
        // Given
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> itemService.update(999L, testItem));
        verify(itemRepository).findById(999L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void delete_WhenItemExists_ShouldDeleteItem() {
        // Given
        when(itemRepository.existsById(1L)).thenReturn(true);

        // When
        itemService.delete(1L);

        // Then
        verify(itemRepository).existsById(1L);
        verify(itemRepository).deleteById(1L);
    }

    @Test
    void delete_WhenItemNotFound_ShouldThrowException() {
        // Given
        when(itemRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> itemService.delete(999L));
        verify(itemRepository).existsById(999L);
        verify(itemRepository, never()).deleteById(any());
    }
}

