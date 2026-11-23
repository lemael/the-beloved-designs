package com.beloveddesigns.admin.service;

import com.beloveddesigns.admin.dto.CreateRestockRequest;
import com.beloveddesigns.admin.dto.RestockResponse;
import com.beloveddesigns.except.BadRequestException;
import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.except.ResourceNotFoundException;
import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.entity.ProductVariant;
import com.beloveddesigns.product.repository.ProductRepository;
import com.beloveddesigns.product.repository.ProductVariantRepository;
import com.beloveddesigns.restock.entity.Restock;
import com.beloveddesigns.restock.entity.RestockItem;
import com.beloveddesigns.restock.entity.RestockStatus;
import com.beloveddesigns.restock.repository.RestockRepository;
import com.beloveddesigns.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRestockService {

    private final RestockRepository restockRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    public Page<RestockResponse> list(RestockStatus status, Pageable pageable) {
        Page<Restock> page = status != null
                ? restockRepository.findByStatus(status, pageable)
                : restockRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public RestockResponse create(CreateRestockRequest request, String adminPhone) {
        Restock restock = Restock.builder()
                .expectedDate(request.expectedDate())
                .supplier(blankToNull(request.supplier()))
                .notes(blankToNull(request.notes()))
                .createdBy(userRepository.findByPhone(adminPhone).orElse(null))
                .build();

        for (CreateRestockRequest.Item item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFoundById(item.productId())));
            ProductVariant variant = null;
            if (item.variantId() != null) {
                variant = variantRepository.findById(item.variantId())
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.variantNotFound(item.variantId())));
                if (!variant.getProduct().getId().equals(product.getId())) {
                    throw new BadRequestException(ErrorMessages.variantNotInProduct(variant.getId(), product.getId()));
                }
            }
            restock.addItem(RestockItem.builder()
                    .product(product)
                    .variant(variant)
                    .quantity(item.quantity())
                    .unitCost(item.unitCost())
                    .build());
        }

        return toResponse(restockRepository.save(restock));
    }

    /**
     * PREVU → RECU : incrémente le stock produit (et variante si précisée), dans la même transaction.
     * Verrou pessimiste sur le ravitaillement : deux clics simultanés ne peuvent pas doubler le stock.
     */
    @Transactional
    public RestockResponse receive(Long id) {
        Restock restock = lockPending(id);
        for (RestockItem item : restock.getItems()) {
            productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
            if (item.getVariant() != null) {
                variantRepository.incrementStock(item.getVariant().getId(), item.getQuantity());
            }
        }
        restock.setStatus(RestockStatus.RECU);
        restock.setReceivedAt(Instant.now());
        return toResponse(restock);
    }

    @Transactional
    public RestockResponse cancel(Long id) {
        Restock restock = lockPending(id);
        restock.setStatus(RestockStatus.ANNULE);
        return toResponse(restock);
    }

    private Restock lockPending(Long id) {
        Restock restock = restockRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.restockNotFound(id)));
        if (restock.getStatus() != RestockStatus.PREVU) {
            throw new BadRequestException(ErrorMessages.restockNotPending(id, restock.getStatus().name()));
        }
        return restock;
    }

    private RestockResponse toResponse(Restock restock) {
        var items = restock.getItems().stream()
                .map(i -> new RestockResponse.Item(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getVariant() != null ? i.getVariant().getId() : null,
                        i.getVariant() != null ? variantLabel(i.getVariant()) : null,
                        i.getQuantity(),
                        i.getUnitCost()))
                .toList();

        int totalQuantity = items.stream().mapToInt(RestockResponse.Item::quantity).sum();
        boolean hasCost = items.stream().anyMatch(i -> i.unitCost() != null);
        BigDecimal totalCost = hasCost
                ? items.stream()
                    .filter(i -> i.unitCost() != null)
                    .map(i -> i.unitCost().multiply(BigDecimal.valueOf(i.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        return new RestockResponse(
                restock.getId(),
                restock.getExpectedDate(),
                restock.getReceivedAt(),
                restock.getSupplier(),
                restock.getStatus(),
                restock.getNotes(),
                restock.getCreatedAt(),
                totalQuantity,
                totalCost,
                items
        );
    }

    private static String variantLabel(ProductVariant v) {
        return Stream.of(v.getSize(), v.getColor())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
