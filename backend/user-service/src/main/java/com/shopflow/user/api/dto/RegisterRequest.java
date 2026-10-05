package com.shopflow.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        @Size(max = 180, message = "Email maksimal 180 karakter")
        String email,

        @NotBlank(message = "Password wajib diisi")
        @Size(min = 8, max = 72, message = "Password harus 8-72 karakter")
        String password,

        @NotBlank(message = "Nama lengkap wajib diisi")
        @Size(max = 120, message = "Nama maksimal 120 karakter")
        String fullName) {
}
