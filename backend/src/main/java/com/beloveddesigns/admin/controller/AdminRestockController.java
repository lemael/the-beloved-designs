package com.beloveddesigns.admin.controller;

import com.beloveddesigns.admin.dto.CreateRestockRequest;
import com.beloveddesigns.admin.dto.RestockResponse;
import com.beloveddesigns.admin.service.AdminRestockService;
import com.beloveddesigns.restock.entity.RestockStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/restocks")
@RequiredArgsConstructor
public class AdminRestockController {

    private final AdminRestockService adminRestockService;

    /** ?status=PREVU pour les prochains ravitaillements (triés par date prévue la plus proche). */
    @GetMapping
    public Page<RestockResponse> list(
            @RequestParam(required = false) RestockStatus status,
            @PageableDefault(size = 20, sort = "expectedDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return adminRestockService.list(status, pageable);
    }

    @PostMapping
    public ResponseEntity<RestockResponse> create(
            @Valid @RequestBody CreateRestockRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminRestockService.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/receive")
    public RestockResponse receive(@PathVariable Long id) {
        return adminRestockService.receive(id);
    }

    @PostMapping("/{id}/cancel")
    public RestockResponse cancel(@PathVariable Long id) {
        return adminRestockService.cancel(id);
    }
}
