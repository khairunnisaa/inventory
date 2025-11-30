package com.example.warehouse.controller;

import com.example.warehouse.dto.ItemVariantDto;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.service.ItemVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/variants")
@RequiredArgsConstructor
public class ItemVariantController {

    private final ItemVariantService variantService;

    @GetMapping
    public List<ItemVariantDto> getAll() {
        return variantService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ItemVariantDto getById(@PathVariable Long id) {
        return convertToDto(variantService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemVariantDto create(@Valid @RequestBody ItemVariant variant) {
        return convertToDto(variantService.create(variant));
    }

    @PutMapping("/{id}")
    public ItemVariantDto update(@PathVariable Long id, @Valid @RequestBody ItemVariant variant) {
        return convertToDto(variantService.update(id, variant));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        variantService.delete(id);
    }

    private ItemVariantDto convertToDto(ItemVariant variant) {
        return ItemVariantDto.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .name(variant.getName())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .itemId(variant.getItem() != null ? variant.getItem().getId() : null)
                .build();
    }
}

