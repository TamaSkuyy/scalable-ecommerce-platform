package com.shopflow.order.api.dto;

import com.shopflow.order.domain.ShippingAddress;

public record ShippingAddressResponse(
        String recipientName,
        String phone,
        String line1,
        String line2,
        String city,
        String postalCode,
        String country) {

    public static ShippingAddressResponse from(ShippingAddress address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddressResponse(
                address.getRecipientName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getPostalCode(),
                address.getCountry());
    }
}
