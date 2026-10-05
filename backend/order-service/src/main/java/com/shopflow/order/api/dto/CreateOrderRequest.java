package com.shopflow.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body checkout.
 *
 * <p>Perhatikan: TIDAK ada daftar item di sini. Pesanan dibangun dari keranjang di
 * cart-service. Kalau klien mengirim daftar item sendiri, klien bisa memalsukan harga -
 * kesalahan keamanan yang sangat umum di sistem e-commerce pemula.
 */
public record CreateOrderRequest(
        @NotNull(message = "Alamat pengiriman wajib diisi")
        @Valid
        ShippingAddressRequest shippingAddress,

        @Size(max = 500, message = "Catatan maksimal 500 karakter")
        String notes) {
}
