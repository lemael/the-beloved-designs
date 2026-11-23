package com.beloveddesigns.restock.repository;

import com.beloveddesigns.restock.entity.RestockItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestockItemRepository extends JpaRepository<RestockItem, Long> {
}
