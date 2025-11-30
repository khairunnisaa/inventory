package com.example.warehouse.service;

import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.exception.ResourceNotFoundException;
import com.example.warehouse.repository.ItemRepository;
import com.example.warehouse.repository.ItemVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemVariantService {

    private final ItemVariantRepository variantRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<ItemVariant> findAll() {
        log.debug("Finding all item variants");
        return variantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ItemVariant findById(Long id) {
        log.debug("Finding variant by id: {}", id);
        return variantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Variant not found: {}", id);
                    return new ResourceNotFoundException("Item variant not found with id: " + id);
                });
    }

    public ItemVariant create(ItemVariant variant) {
        log.info("Creating new variant: sku={}", variant.getSku());
        Item item = itemRepository.findById(variant.getItem().getId())
                .orElseThrow(() -> {
                    log.warn("Item not found for variant creation: {}", variant.getItem().getId());
                    return new ResourceNotFoundException("Item not found with id: " + variant.getItem().getId());
                });
        validateVariant(variant);
        variant.setItem(item);
        item.setHasVariants(true);
        itemRepository.save(item);
        ItemVariant saved = variantRepository.save(variant);
        log.info("Variant created successfully: id={}, sku={}", saved.getId(), saved.getSku());
        return saved;
    }

    public ItemVariant update(Long id, ItemVariant updated) {
        log.info("Updating variant: id={}", id);
        ItemVariant existing = findById(id);
        validateVariant(updated);
        existing.setSku(updated.getSku());
        existing.setName(updated.getName());
        existing.setPrice(updated.getPrice());
        existing.setStockQuantity(updated.getStockQuantity());
        ItemVariant saved = variantRepository.save(existing);
        log.info("Variant updated successfully: id={}, sku={}", saved.getId(), saved.getSku());
        return saved;
    }

    public void delete(Long id) {
        log.info("Deleting variant: id={}", id);
        if (!variantRepository.existsById(id)) {
            log.warn("Variant not found for deletion: {}", id);
            throw new ResourceNotFoundException("Item variant not found with id: " + id);
        }
        variantRepository.deleteById(id);
        log.info("Variant deleted successfully: id={}", id);
    }

    private void validateVariant(ItemVariant variant) {
        if (variant.getStockQuantity() != null && variant.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        if (variant.getPrice() != null && variant.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }
}

