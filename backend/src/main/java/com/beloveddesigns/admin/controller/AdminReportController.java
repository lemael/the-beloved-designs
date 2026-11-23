package com.beloveddesigns.admin.controller;

import com.beloveddesigns.admin.dto.DashboardResponse;
import com.beloveddesigns.admin.dto.YearlyReportResponse;
import com.beloveddesigns.admin.service.AdminReportService;
import com.beloveddesigns.admin.service.AdminStockService;
import com.beloveddesigns.except.BadRequestException;
import com.beloveddesigns.except.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReportController {

    private static final int MIN_YEAR = 2000;

    private final AdminReportService adminReportService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(defaultValue = "" + AdminStockService.DEFAULT_LOW_STOCK_THRESHOLD) int threshold
    ) {
        AdminStockController.checkThreshold(threshold);
        return adminReportService.dashboard(threshold);
    }

    /** Sans ?year : année en cours (fuseau du Cameroun). */
    @GetMapping("/reports/yearly")
    public YearlyReportResponse yearly(@RequestParam(required = false) Integer year) {
        int currentYear = LocalDate.now(AdminReportService.SHOP_ZONE).getYear();
        int y = year != null ? year : currentYear;
        if (y < MIN_YEAR || y > currentYear + 1) {
            throw new BadRequestException(ErrorMessages.INVALID_YEAR);
        }
        return adminReportService.yearly(y);
    }
}
