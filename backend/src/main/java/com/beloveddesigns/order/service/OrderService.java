package com.beloveddesigns.order.service;

import com.beloveddesigns.cart.entity.Cart;
import com.beloveddesigns.cart.entity.CartItem;
import com.beloveddesigns.cart.repository.CartRepository;
import com.beloveddesigns.except.EmptyCartException;
import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.except.InsufficientStockException;
import com.beloveddesigns.except.ProductUnavailableException;
import com.beloveddesigns.except.ResourceNotFoundException;
import com.beloveddesigns.order.dto.OrderResponse;
import com.beloveddesigns.order.dto.PlaceOrderRequest;
import com.beloveddesigns.order.entity.Order;
import com.beloveddesigns.order.entity.OrderItem;
import com.beloveddesigns.order.entity.OrderStatus;
import com.beloveddesigns.order.repository.OrderRepository;
import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.repository.ProductRepository;
import com.beloveddesigns.product.service.ProductService;
import com.beloveddesigns.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * « Passer la commande » (décision du 2026-11-25, voir ai-memory/memory/decisions.md).
 *
 * <p>Tout se fait dans <b>une seule transaction</b> : si un article pose problème (stock, produit
 * désactivé), rien n'est modifié — ni stock, ni panier, ni commande.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final OrderPricing orderPricing;
    private final OrderWhatsappLink whatsappLink;

    public OrderResponse placeOrder(User user, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(EmptyCartException::new);

        // Verrouille les produits du panier (SELECT … FOR UPDATE) : stock relu à jour et protégé
        // jusqu'à la fin de la transaction contre une commande simultanée.
        List<Long> productIds = cart.getItems().stream().map(item -> item.getProduct().getId()).toList();
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 1) Tout vérifier avant de modifier quoi que ce soit
        for (CartItem cartItem : cart.getItems()) {
            Product product = products.get(cartItem.getProduct().getId());
            if (product == null || !product.isActive()) {
                throw new ProductUnavailableException(cartItem.getProduct().getName());
            }
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new InsufficientStockException(product.getStockQuantity());
            }
        }

        // 2) Lignes au prix du jour (une promo a pu commencer ou finir depuis l'ajout au panier)
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .deliveryCity(request.deliveryCity().trim())
                .deliveryDistrict(request.deliveryDistrict().trim())
                .deliveryContactPhone(request.contactPhone())
                .build();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = products.get(cartItem.getProduct().getId());
            BigDecimal unitPrice = productService.currentPrice(product);
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .build());
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // 3) Montants : réduction de première commande, livraison, total
        boolean firstOrder = !orderRepository.existsByUserIdAndStatusNot(user.getId(), OrderStatus.CANCELLED);
        OrderPricing.Amounts amounts = orderPricing.compute(subtotal, firstOrder, order.getDeliveryCity());
        order.setSubtotal(amounts.subtotal());
        order.setFirstOrderDiscount(amounts.firstOrderDiscount());
        order.setDeliveryFee(amounts.deliveryFee());
        order.setTotalAmount(amounts.total());
        orderRepository.save(order);

        // 4) Stock décrémenté, panier vidé
        for (CartItem cartItem : cart.getItems()) {
            Product product = products.get(cartItem.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
        }
        cart.getItems().clear();

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(User user, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.orderNotFound(orderId)));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(User user, Pageable pageable) {
        return orderRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getFirstOrderDiscount(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                new OrderResponse.Delivery(order.getDeliveryCity(), order.getDeliveryDistrict(), order.getDeliveryContactPhone()),
                order.getItems().stream()
                        .map(item -> new OrderResponse.Item(
                                item.getProduct().getId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                        .toList(),
                whatsappLink.linkFor(order)
        );
    }
}
