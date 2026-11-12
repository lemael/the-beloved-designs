package com.beloveddesigns.cart.service;

import com.beloveddesigns.cart.dto.AddCartItemRequest;
import com.beloveddesigns.cart.dto.CartItemResponse;
import com.beloveddesigns.cart.dto.CartResponse;
import com.beloveddesigns.cart.dto.UpdateCartItemRequest;
import com.beloveddesigns.cart.entity.Cart;
import com.beloveddesigns.cart.entity.CartItem;
import com.beloveddesigns.cart.repository.CartItemRepository;
import com.beloveddesigns.cart.repository.CartRepository;
import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.except.ResourceNotFoundException;
import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.service.ProductService;
import com.beloveddesigns.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    public CartResponse getCart(User user) {
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    public CartItemResponse addItem(User user, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(user);
        Product product = productService.getProduct(request.productId());

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.addQuantity(request.quantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElseThrow();

        return new CartItemResponse(
                item.getId(),
                productService.toResponse(item.getProduct()),
                item.getQuantity()
        );
    }

    public CartItemResponse updateItem(User user, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = findCartForUser(user);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return new CartItemResponse(
                item.getId(),
                productService.toResponse(item.getProduct()),
                item.getQuantity()
        );
    }

    public void removeItem(User user, Long cartItemId) {
        Cart cart = findCartForUser(user);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    public void clearCart(User user) {
        Cart cart = findCartForUser(user);
        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    private Cart findCartForUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toResponse(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId(),
                cart.getItems().stream()
                        .map(item -> new CartItemResponse(
                                item.getId(),
                                productService.toResponse(item.getProduct()),
                                item.getQuantity()
                        ))
                        .toList(),
                total
        );
    }
}
