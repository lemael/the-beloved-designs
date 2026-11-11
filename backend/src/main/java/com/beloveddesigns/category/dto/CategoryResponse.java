package com.beloveddesigns.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Long parentId
) {
}
