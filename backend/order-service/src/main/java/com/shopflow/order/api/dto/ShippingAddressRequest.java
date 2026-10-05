package com.shopflow.order.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
        @NotBlank(message = "Nama penerima wajib diisi")
        @Size(max = 120, message = "Nama penerima maksimal 120 karakter")
        String recipientName,

        @NotBlank(message = "Nomor telepon wajib diisi")
        @Size(max = 32, message = "Nomor telepon maksimal 32 karakter")
        String phone,

        @NotBlank(message = "Alamat wajib diisi")
        @Size(max = 200, message = "Alamat maksimal 200 karakter")
        String line1,

        @Size(max = 200, message = "Alamat baris 2 maksimal 200 karakter")
        String line2,

        @NotBlank(message = "Kota wajib diisi")
        @Size(max = 120, message = "Kota maksimal 120 karakter")
        String city,

        @NotBlank(message = "Kode pos wajib diisi")
        @Size(max = 16, message = "Kode pos maksimal 16 karakter")
        String postalCode,

        @NotBlank(message = "Negara wajib diisi")
        @Size(max = 64, message = "Negara maksimal 64 karakter")
        String country) {
}
