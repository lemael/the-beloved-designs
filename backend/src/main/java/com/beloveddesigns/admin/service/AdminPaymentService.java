package com.beloveddesigns.admin.service;

import com.beloveddesigns.admin.dto.ConfirmPaymentRequest;
import com.beloveddesigns.admin.dto.DeliveryFeeQuoteResponse;
import com.beloveddesigns.admin.dto.OrderPaymentPrefillResponse;
import com.beloveddesigns.admin.dto.PaymentConfirmationResponse;
import com.beloveddesigns.except.BadRequestException;
import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.except.InsufficientStockException;
import com.beloveddesigns.except.OrderNotPendingException;
import com.beloveddesigns.except.ProductUnavailableException;
import com.beloveddesigns.except.ResourceNotFoundException;
import com.beloveddesigns.order.dto.OrderResponse;
import com.beloveddesigns.order.entity.Order;
import com.beloveddesigns.order.entity.OrderItem;
import com.beloveddesigns.order.entity.OrderStatus;
import com.beloveddesigns.order.repository.OrderRepository;
import com.beloveddesigns.order.service.OrderPricing;
import com.beloveddesigns.payment.entity.Payment;
import com.beloveddesigns.payment.repository.PaymentRepository;
import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.repository.ProductRepository;
import com.beloveddesigns.product.service.ProductService;
import com.beloveddesigns.user.entity.Role;
import com.beloveddesigns.user.entity.User;
import com.beloveddesigns.user.repository.RoleRepository;
import com.beloveddesigns.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Back-office « Confirmation paiement » (décision du 2026-11-25, voir ai-memory/memory/decisions.md).
 *
 * <p>Deux cas, chacun en <b>une transaction</b> (tout ou rien) :
 * <ul>
 *   <li><b>commande du site</b> (orderId) : doit être PENDING ; si l'admin change produits ou quantités,
 *       le stock est ajusté de la différence ; la commande passe PAID ;</li>
 *   <li><b>vente directe</b> : commande PAID créée au prix du jour, stock décrémenté ; client retrouvé par
 *       son numéro WhatsApp ou compte créé sans mot de passe utilisable.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminPaymentService {

    /**
     * Ne correspond à aucun hash BCrypt : connexion impossible tant que le client n'a pas défini de
     * mot de passe (même convention que les comptes importés depuis Excel, data/publisher/publish.py).
     */
    static final String NO_PASSWORD = "!compte-cree-par-admin-sans-mot-de-passe";
    private static final String CLIENT_ROLE = "CLIENT";
    private static final String PAYMENT_CONFIRMED = "CONFIRMED";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderPricing orderPricing;
    private final PaymentReceiptWhatsapp receipt;

    // ---- Pré-remplissage et tarif -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderPaymentPrefillResponse prefill(Long orderId) {
        Order order = findOrder(orderId);
        return new OrderPaymentPrefillResponse(
                order.getId(),
                order.getStatus(),
                order.getDeliveryContactPhone() != null ? order.getDeliveryContactPhone() : order.getUser().getPhone(),
                order.getDeliveryCity(),
                order.getDeliveryDistrict(),
                order.getDeliveryFee(),
                order.getSubtotal(),
                order.getFirstOrderDiscount(),
                order.getTotalAmount(),
                order.getItems().stream()
                        .map(item -> new OrderPaymentPrefillResponse.Item(
                                item.getProduct().getId(),
                                item.getProductName(),
                                item.getProduct().getCategories().stream().map(c -> c.getSlug()).sorted().findFirst().orElse(null),
                                item.getQuantity(),
                                item.getUnitPrice()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public DeliveryFeeQuoteResponse quoteDeliveryFee(String city, BigDecimal amount) {
        return new DeliveryFeeQuoteResponse(orderPricing.deliveryFee(amount, city));
    }

    // ---- Confirmation ---------------------------------------------------------------------------

    public PaymentConfirmationResponse confirm(ConfirmPaymentRequest request) {
        Map<Long, Integer> wanted = mergeLines(request.items());
        Order order = request.orderId() != null
                ? applyToSiteOrder(findOrder(request.orderId()), wanted)
                : createDirectSale(request.customerWhatsapp(), wanted);

        BigDecimal subtotal = order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = request.discount() == null ? BigDecimal.ZERO : request.discount();
        if (discount.compareTo(subtotal.subtract(order.getFirstOrderDiscount())) > 0) {
            throw new BadRequestException(ErrorMessages.DISCOUNT_ABOVE_SUBTOTAL);
        }

        order.setSubtotal(subtotal);
        order.setManualDiscount(discount);
        order.setDeliveryFee(request.deliveryFee());
        order.setTotalAmount(subtotal.subtract(order.getFirstOrderDiscount()).subtract(discount).add(request.deliveryFee()));
        if (request.deliveryCity() != null && !request.deliveryCity().isBlank()) {
            order.setDeliveryCity(request.deliveryCity().trim());
        }
        if (request.deliveryDistrict() != null && !request.deliveryDistrict().isBlank()) {
            order.setDeliveryDistrict(request.deliveryDistrict().trim());
        }
        order.setDeliveryContactPhone(request.customerWhatsapp());
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .provider(request.paymentMethod().name())
                .status(PAYMENT_CONFIRMED)
                .amount(request.amountPaid())
                .currency(order.getCurrency())
                .paidOn(request.paidOn())
                .build());

        return toResponse(order, payment, request);
    }

    /** Commande du site : stock ajusté de la différence (rendu / pris), lignes mises à jour. */
    private Order applyToSiteOrder(Order order, Map<Long, Integer> wanted) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPendingException(order.getId(), order.getStatus().name());
        }
        Map<Long, OrderItem> current = order.getItems().stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), Function.identity()));
        Set<Long> allIds = new HashSet<>(current.keySet());
        allIds.addAll(wanted.keySet());
        Map<Long, Product> products = lockProducts(allIds);

        // 1) Tout vérifier avant de modifier
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product product = products.get(entry.getKey());
            int already = current.containsKey(entry.getKey()) ? current.get(entry.getKey()).getQuantity() : 0;
            int extra = entry.getValue() - already;
            if (extra > 0 && !product.isActive()) {
                throw new ProductUnavailableException(product.getName());
            }
            if (extra > product.getStockQuantity()) {
                throw new InsufficientStockException(product.getStockQuantity());
            }
        }

        // 2) Ajuster le stock et les lignes
        for (Long productId : allIds) {
            Product product = products.get(productId);
            int before = current.containsKey(productId) ? current.get(productId).getQuantity() : 0;
            int after = wanted.getOrDefault(productId, 0);
            product.setStockQuantity(product.getStockQuantity() - (after - before));

            OrderItem line = current.get(productId);
            if (after == 0) {
                order.getItems().remove(line);
            } else if (line != null) {
                line.setQuantity(after);   // prix convenu à la commande conservé
            } else {
                order.getItems().add(newLine(order, product, after));
            }
        }

        // -10 % de première commande : recalculé sur le nouveau sous-total s'il avait été accordé
        if (order.getFirstOrderDiscount().signum() > 0) {
            BigDecimal subtotal = order.getItems().stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setFirstOrderDiscount(orderPricing.firstOrderDiscount(subtotal));
        }
        return order;
    }

    /** Vente directe : tout est vérifié avant de créer le compte client, pour ne rien créer en cas de refus. */
    private Order createDirectSale(String customerWhatsapp, Map<Long, Integer> wanted) {
        Map<Long, Product> products = lockProducts(wanted.keySet());
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product product = products.get(entry.getKey());
            if (!product.isActive()) {
                throw new ProductUnavailableException(product.getName());
            }
            if (entry.getValue() > product.getStockQuantity()) {
                throw new InsufficientStockException(product.getStockQuantity());
            }
        }

        Order order = Order.builder()
                .user(findOrCreateCustomer(customerWhatsapp))
                .status(OrderStatus.PENDING)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();
        wanted.forEach((productId, quantity) -> {
            Product product = products.get(productId);
            product.setStockQuantity(product.getStockQuantity() - quantity);
            order.getItems().add(newLine(order, product, quantity));
        });
        return order;
    }

    private OrderItem newLine(Order order, Product product, int quantity) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .quantity(quantity)
                .unitPrice(productService.currentPrice(product))
                .build();
    }

    private User findOrCreateCustomer(String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            Role client = roleRepository.findByName(CLIENT_ROLE)
                    .orElseThrow(() -> new IllegalStateException(ErrorMessages.defaultRoleMissing(CLIENT_ROLE)));
            User user = User.builder()
                    .phone(phone)
                    .passwordHash(NO_PASSWORD)
                    .firstName("Client")
                    .lastName("WhatsApp")
                    .build();
            user.getRoles().add(client);
            return userRepository.save(user);
        });
    }

    /** Produits verrouillés (SELECT … FOR UPDATE) ; un identifiant inconnu → 404. */
    private Map<Long, Product> lockProducts(Set<Long> ids) {
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (Long id : ids) {
            if (!products.containsKey(id)) {
                throw new ResourceNotFoundException(ErrorMessages.productNotFoundById(id));
            }
        }
        return products;
    }

    /** Deux sous-formulaires pour le même produit : quantités additionnées. */
    private static Map<Long, Integer> mergeLines(List<ConfirmPaymentRequest.Line> lines) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        lines.forEach(line -> merged.merge(line.productId(), line.quantity(), Integer::sum));
        return merged;
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.orderNotFound(orderId)));
    }

    private PaymentConfirmationResponse toResponse(Order order, Payment payment, ConfirmPaymentRequest request) {
        return new PaymentConfirmationResponse(
                order.getId(),
                payment.getId(),
                order.getStatus(),
                request.customerWhatsapp(),
                order.getDeliveryCity(),
                order.getDeliveryDistrict(),
                order.getItems().stream()
                        .map(i -> new OrderResponse.Item(i.getProduct().getId(), i.getProductName(), i.getQuantity(),
                                i.getUnitPrice(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                        .toList(),
                order.getSubtotal(),
                order.getFirstOrderDiscount(),
                order.getManualDiscount(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                payment.getAmount(),
                request.paymentMethod(),
                payment.getPaidOn(),
                receipt.linkFor(order, payment, request.customerWhatsapp()));
    }
}
