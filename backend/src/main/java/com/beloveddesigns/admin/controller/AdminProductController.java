package com.beloveddesigns.admin.controller;

import com.beloveddesigns.admin.dto.AdminProductResponse;
import com.beloveddesigns.admin.dto.CreateProductRequest;
import com.beloveddesigns.admin.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Accès réservé au rôle ADMIN (SecurityConfig : /api/admin/**). */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    /** ?category=femmes&category=hommes (ou category=femmes,hommes) — sans paramètre : tous les produits. */
    @GetMapping
    public Page<AdminProductResponse> list(
            @RequestParam(name = "category", required = false) List<String> categories,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return adminProductService.list(categories, pageable);
    }

    @PostMapping
    public ResponseEntity<AdminProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminProductService.create(request));
    }
}
