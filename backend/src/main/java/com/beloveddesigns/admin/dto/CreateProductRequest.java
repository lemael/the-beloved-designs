package com.beloveddesigns.admin.dto;

import com.beloveddesigns.except.ErrorMessages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank(message = ErrorMessages.PRODUCT_NAME_REQUIRED) @Size(max = 255) String name,
        /** Optionnel : généré à partir du nom si absent. */
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = ErrorMessages.INVALID_SLUG_FORMAT)
        @Size(max = 255) String slug,
        String description,
        @NotNull(message = ErrorMessages.PRODUCT_PRICE_REQUIRED)
        @Positive(message = ErrorMessages.PRODUCT_PRICE_POSITIVE) BigDecimal price,
        @PositiveOrZero(message = ErrorMessages.STOCK_NOT_NEGATIVE) Integer stockQuantity,
        Boolean featured,
        Boolean active,
        @NotEmpty(message = ErrorMessages.PRODUCT_CATEGORY_REQUIRED) List<@NotBlank String> categorySlugs,
        List<@NotBlank @Size(max = 500) String> imageUrls,
        List<@Valid VariantRequest> variants
) {
    public record VariantRequest(
            @Size(max = 50) String size,
            @Size(max = 50) String color,
            @PositiveOrZero(message = ErrorMessages.STOCK_NOT_NEGATIVE) Integer stockQuantity
    ) {
    }
}
