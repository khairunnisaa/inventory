package com.example.warehouse.controller;

import com.example.warehouse.dto.ItemDto;
import com.example.warehouse.dto.ItemVariantDto;
import com.example.warehouse.entity.Item;
import com.example.warehouse.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemDto> getAll() {
        return itemService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ItemDto getById(@PathVariable Long id) {
        return convertToDto(itemService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody Item item) {
        return convertToDto(itemService.create(item));
    }

    @PutMapping("/{id}")
    public ItemDto update(@PathVariable Long id, @Valid @RequestBody Item item) {
        return convertToDto(itemService.update(id, item));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        itemService.delete(id);
    }

    private ItemDto convertToDto(Item item) {
        List<ItemVariantDto> variantDtos = null;
        if (item.getVariants() != null) {
            variantDtos = item.getVariants().stream()
                    .map(variant -> ItemVariantDto.builder()
                            .id(variant.getId())
                            .sku(variant.getSku())
                            .name(variant.getName())
                            .price(variant.getPrice())
                            .stockQuantity(variant.getStockQuantity())
                            .itemId(variant.getItem().getId())
                            .build())
                    .collect(Collectors.toList());
        }

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .basePrice(item.getBasePrice())
                .stockQuantity(item.getStockQuantity())
                .hasVariants(item.getHasVariants())
                .variants(variantDtos)
                .build();
    }
}

