package com.beloveddesigns.admin.controller;

import com.beloveddesigns.admin.dto.StockSummaryResponse;
import com.beloveddesigns.admin.service.AdminStockService;
import com.beloveddesigns.except.BadRequestException;
import com.beloveddesigns.except.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    static final int MAX_THRESHOLD = 1000;

    private final AdminStockService adminStockService;

    @GetMapping("/summary")
    public StockSummaryResponse summary(
            @RequestParam(defaultValue = "" + AdminStockService.DEFAULT_LOW_STOCK_THRESHOLD) int threshold
    ) {
        checkThreshold(threshold);
        return adminStockService.summary(threshold);
    }

    static void checkThreshold(int threshold) {
        if (threshold < 0 || threshold > MAX_THRESHOLD) {
            throw new BadRequestException(ErrorMessages.INVALID_STOCK_THRESHOLD);
        }
    }
}
