package com.beloveddesigns.order.controller;

import com.beloveddesigns.order.dto.OrderResponse;
import com.beloveddesigns.order.dto.PlaceOrderRequest;
import com.beloveddesigns.order.service.OrderService;
import com.beloveddesigns.security.CurrentUser;
import com.beloveddesigns.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Commandes du client connecté (route protégée par SecurityConfig : non connecté → 401). */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** Passe la commande à partir du panier ; la réponse contient le lien WhatsApp vers la boutique. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@CurrentUser User user, @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(user, request);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@CurrentUser User user, @PathVariable Long id) {
        return orderService.getOrder(user, id);
    }

    @GetMapping
    public Page<OrderResponse> listOrders(@CurrentUser User user,
                                          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                          Pageable pageable) {
        return orderService.listOrders(user, pageable);
    }
}
