package com.beloveddesigns.order.dto;

import com.beloveddesigns.except.ErrorMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** « Passer la commande » : où livrer et qui appeler. Le contenu vient du panier du client. */
public record PlaceOrderRequest(
        @NotBlank(message = ErrorMessages.DELIVERY_CITY_REQUIRED) @Size(max = 100) String deliveryCity,
        @NotBlank(message = ErrorMessages.DELIVERY_DISTRICT_REQUIRED) @Size(max = 150) String deliveryDistrict,
        /** Numéro WhatsApp que le livreur appellera, format international (+237…). */
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = ErrorMessages.INVALID_PHONE_FORMAT)
        String contactPhone
) {
}
