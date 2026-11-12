package com.beloveddesigns.cart.controller;

import com.beloveddesigns.cart.dto.AddCartItemRequest;
import com.beloveddesigns.cart.dto.CartItemResponse;
import com.beloveddesigns.cart.dto.CartResponse;
import com.beloveddesigns.cart.dto.UpdateCartItemRequest;
import com.beloveddesigns.cart.service.CartService;
import com.beloveddesigns.security.CurrentUser;
import com.beloveddesigns.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@CurrentUser User user) {
        return ResponseEntity.ok(cartService.getCart(user));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
            @CurrentUser User user,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(user, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItemResponse> updateItem(
            @CurrentUser User user,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ResponseEntity.ok(cartService.updateItem(user, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @CurrentUser User user,
            @PathVariable Long itemId
    ) {
        cartService.removeItem(user, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@CurrentUser User user) {
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}
