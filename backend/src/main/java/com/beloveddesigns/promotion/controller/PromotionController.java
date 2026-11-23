package com.beloveddesigns.promotion.controller;

import com.beloveddesigns.promotion.dto.PromotionResponse;
import com.beloveddesigns.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public (SecurityConfig : /api/promotions/** en permitAll). */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public List<PromotionResponse> findCurrent() {
        return promotionService.findCurrent();
    }
}
