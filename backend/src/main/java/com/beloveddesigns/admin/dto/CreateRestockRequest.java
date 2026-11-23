package com.beloveddesigns.admin.dto;

import com.beloveddesigns.except.ErrorMessages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateRestockRequest(
        @NotNull(message = ErrorMessages.RESTOCK_DATE_REQUIRED) LocalDate expectedDate,
        @Size(max = 150) String supplier,
        String notes,
        @NotEmpty(message = ErrorMessages.RESTOCK_ITEMS_REQUIRED) List<@Valid @NotNull Item> items
) {
    public record Item(
            @NotNull(message = ErrorMessages.RESTOCK_PRODUCT_REQUIRED) Long productId,
            Long variantId,
            @NotNull(message = ErrorMessages.QUANTITY_POSITIVE)
            @Positive(message = ErrorMessages.QUANTITY_POSITIVE) Integer quantity,
            @PositiveOrZero(message = ErrorMessages.UNIT_COST_NOT_NEGATIVE) BigDecimal unitCost
    ) {
    }
}
