package com.example.warehouse.service;

import com.example.warehouse.dto.CreateOrderRequest;
import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.entity.Order;
import com.example.warehouse.entity.OrderLine;
import com.example.warehouse.exception.InsufficientStockException;
import com.example.warehouse.exception.ResourceNotFoundException;
import com.example.warehouse.repository.ItemRepository;
import com.example.warehouse.repository.ItemVariantRepository;
import com.example.warehouse.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class OrderService {

    // Helper class to track stock updates
    private static class StockUpdate {
        final boolean isVariant;
        final Long entityId;
        final Integer quantity;

        StockUpdate(boolean isVariant, Long entityId, Integer quantity) {
            this.isVariant = isVariant;
            this.entityId = entityId;
            this.quantity = quantity;
        }
    }

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final ItemVariantRepository variantRepository;

    public Order createOrder(CreateOrderRequest request) {
        log.info("Creating order with {} line(s)", request.getLines().size());
        
        // Validate no duplicate order lines
        validateNoDuplicateLines(request.getLines());
        
        // First pass: validation and stock check
        List<OrderLine> orderLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        List<StockUpdate> stockUpdates = new ArrayList<>();

        for (CreateOrderRequest.OrderLineRequest lineReq : request.getLines()) {
            Item item = itemRepository.findById(lineReq.getItemId())
                    .orElseThrow(() -> {
                        log.warn("Item not found: {}", lineReq.getItemId());
                        return new ResourceNotFoundException("Item not found with id: " + lineReq.getItemId());
                    });

            ItemVariant variant = null;
            BigDecimal unitPrice;
            Integer availableStock;
            String itemIdentifier;

            if (lineReq.getVariantId() != null) {
                variant = variantRepository.findById(lineReq.getVariantId())
                        .orElseThrow(() -> {
                            log.warn("Variant not found: {}", lineReq.getVariantId());
                            return new ResourceNotFoundException("Item variant not found with id: " + lineReq.getVariantId());
                        });

                if (!variant.getItem().getId().equals(item.getId())) {
                    log.error("Variant {} does not belong to item {}", variant.getId(), item.getId());
                    throw new IllegalArgumentException("Variant does not belong to item with id: " + item.getId());
                }

                availableStock = variant.getStockQuantity();
                unitPrice = variant.getPrice();
                itemIdentifier = String.format("%s (Variant: %s)", item.getName(), variant.getName());
                
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException("Invalid price for variant: " + variant.getId());
                }
            } else {
                if (item.getHasVariants()) {
                    log.error("Item {} has variants but no variant ID provided", item.getId());
                    throw new IllegalArgumentException("Item has variants. Please specify a variant ID.");
                }
                availableStock = item.getStockQuantity();
                if (item.getBasePrice() == null) {
                    throw new IllegalStateException("Item does not have a base price");
                }
                unitPrice = item.getBasePrice();
                itemIdentifier = item.getName();
                
                if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException("Invalid base price for item: " + item.getId());
                }
            }

            // Validate quantity
            if (lineReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for " + itemIdentifier);
            }

            // Check stock availability
            if (availableStock < lineReq.getQuantity()) {
                log.warn("Insufficient stock for {}: Available={}, Requested={}", 
                        itemIdentifier, availableStock, lineReq.getQuantity());
                throw new InsufficientStockException(
                        String.format("Not enough stock for %s. Available: %d, Requested: %d",
                                itemIdentifier, availableStock, lineReq.getQuantity()));
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(lineReq.getQuantity()));
            total = total.add(lineTotal);

            OrderLine ol = OrderLine.builder()
                    .item(item)
                    .variant(variant)
                    .quantity(lineReq.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build();
            orderLines.add(ol);
            
            // Track stock updates needed
            stockUpdates.add(new StockUpdate(variant != null, variant != null ? variant.getId() : item.getId(), lineReq.getQuantity()));
        }

        // Second pass: actually reduce stock (after all checks passed)
        // Re-fetch entities to get latest version and prevent race conditions
        try {
            for (StockUpdate update : stockUpdates) {
                if (update.isVariant) {
                    ItemVariant lockedVariant = variantRepository.findById(update.entityId)
                            .orElseThrow(() -> new ResourceNotFoundException("Variant not found during stock update: " + update.entityId));
                    
                    int newStock = lockedVariant.getStockQuantity() - update.quantity;
                    if (newStock < 0) {
                        log.error("Stock would become negative for variant {}: Current={}, Requested={}", 
                                lockedVariant.getId(), lockedVariant.getStockQuantity(), update.quantity);
                        throw new InsufficientStockException("Stock update would result in negative quantity for variant: " + lockedVariant.getName());
                    }
                    lockedVariant.setStockQuantity(newStock);
                    variantRepository.save(lockedVariant);
                    log.debug("Reduced stock for variant {}: New quantity={}", lockedVariant.getId(), newStock);
                } else {
                    Item lockedItem = itemRepository.findById(update.entityId)
                            .orElseThrow(() -> new ResourceNotFoundException("Item not found during stock update: " + update.entityId));
                    
                    int newStock = lockedItem.getStockQuantity() - update.quantity;
                    if (newStock < 0) {
                        log.error("Stock would become negative for item {}: Current={}, Requested={}", 
                                lockedItem.getId(), lockedItem.getStockQuantity(), update.quantity);
                        throw new InsufficientStockException("Stock update would result in negative quantity for item: " + lockedItem.getName());
                    }
                    lockedItem.setStockQuantity(newStock);
                    itemRepository.save(lockedItem);
                    log.debug("Reduced stock for item {}: New quantity={}", lockedItem.getId(), newStock);
                }
            }

            // Save order
            String orderNumber = generateUniqueOrderNumber();
            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .createdAt(Instant.now())
                    .totalAmount(total)
                    .lines(new ArrayList<>())
                    .build();

            order = orderRepository.save(order);

            for (OrderLine line : orderLines) {
                line.setOrder(order);
            }
            order.setLines(orderLines);

            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully: OrderNumber={}, Total={}, Lines={}", 
                    orderNumber, total, orderLines.size());
            return savedOrder;
            
        } catch (OptimisticLockingFailureException e) {
            log.error("Optimistic locking failure during order creation", e);
            throw new IllegalStateException("Order could not be processed due to concurrent modification. Please try again.");
        }
    }

    private void validateNoDuplicateLines(List<CreateOrderRequest.OrderLineRequest> lines) {
        Set<String> seen = new HashSet<>();
        for (CreateOrderRequest.OrderLineRequest line : lines) {
            String key = line.getItemId() + "-" + (line.getVariantId() != null ? line.getVariantId() : "null");
            if (seen.contains(key)) {
                throw new IllegalArgumentException("Duplicate order line detected for item " + line.getItemId() + 
                        (line.getVariantId() != null ? " variant " + line.getVariantId() : ""));
            }
            seen.add(key);
        }
    }

    private String generateUniqueOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        log.debug("Finding all orders");
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        log.debug("Finding order by id: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found: {}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });
    }
}

