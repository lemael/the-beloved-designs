package com.beloveddesigns.category.controller;

import com.beloveddesigns.category.dto.CategoryResponse;
import com.beloveddesigns.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("/{slug}")
    public CategoryResponse findBySlug(@PathVariable String slug) {
        return categoryService.findBySlug(slug);
    }
}
