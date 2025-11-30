package com.example.warehouse.service;

import com.example.warehouse.entity.Item;
import com.example.warehouse.exception.ResourceNotFoundException;
import com.example.warehouse.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<Item> findAll() {
        log.debug("Finding all items");
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Item findById(Long id) {
        log.debug("Finding item by id: {}", id);
        return itemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Item not found: {}", id);
                    return new ResourceNotFoundException("Item not found with id: " + id);
                });
    }

    public Item create(Item item) {
        log.info("Creating new item: {}", item.getName());
        validateItem(item);
        Item saved = itemRepository.save(item);
        log.info("Item created successfully: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public Item update(Long id, Item updated) {
        log.info("Updating item: id={}", id);
        Item existing = findById(id);
        validateItem(updated);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setBasePrice(updated.getBasePrice());
        existing.setStockQuantity(updated.getStockQuantity());
        existing.setHasVariants(updated.getHasVariants());
        Item saved = itemRepository.save(existing);
        log.info("Item updated successfully: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public void delete(Long id) {
        log.info("Deleting item: id={}", id);
        if (!itemRepository.existsById(id)) {
            log.warn("Item not found for deletion: {}", id);
            throw new ResourceNotFoundException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
        log.info("Item deleted successfully: id={}", id);
    }

    private void validateItem(Item item) {
        if (item.getStockQuantity() != null && item.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        if (item.getBasePrice() != null && item.getBasePrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative");
        }
    }
}

