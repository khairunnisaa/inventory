package com.example.warehouse.repository;

import com.example.warehouse.entity.ItemVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemVariantRepository extends JpaRepository<ItemVariant, Long> {
}

