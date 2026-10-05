package com.shopflow.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Nama lengkap wajib diisi")
        @Size(max = 120, message = "Nama maksimal 120 karakter")
        String fullName) {
}
