package com.shopflow.user.api.dto;

import com.shopflow.user.domain.UserAccount;

import java.time.Instant;
import java.util.List;

/** Representasi user yang aman dikirim ke klien (tanpa hash password). */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        List<String> roles,
        Instant createdAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.roleList(),
                user.getCreatedAt());
    }
}
