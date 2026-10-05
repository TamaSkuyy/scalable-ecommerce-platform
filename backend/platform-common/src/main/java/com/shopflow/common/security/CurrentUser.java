package com.shopflow.common.security;

import java.util.Arrays;
import java.util.List;

/**
 * Identitas pemanggil yang sudah diverifikasi Gateway.
 *
 * <p>Dipakai langsung sebagai parameter controller:
 * <pre>
 * &#64;GetMapping("/me")
 * public ProfileResponse me(CurrentUser user) { ... }
 * </pre>
 * Lihat {@link CurrentUserArgumentResolver} yang mengisinya dari header internal.
 */
public record CurrentUser(Long id, String email, List<String> roles) {

    public static final CurrentUser ANONYMOUS = new CurrentUser(null, null, List.of());

    public CurrentUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean isAuthenticated() {
        return id != null;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(SecurityHeaders.ROLE_ADMIN);
    }

    /** Header boleh kosong/rusak: itu artinya anonim, bukan error 500. */
    public static CurrentUser of(String rawId, String rawEmail, String rawRolesCsv) {
        Long userId = null;
        if (rawId != null && !rawId.isBlank()) {
            try {
                userId = Long.valueOf(rawId.trim());
            } catch (NumberFormatException ignored) {
                userId = null;
            }
        }

        String email = (rawEmail == null || rawEmail.isBlank()) ? null : rawEmail.trim();

        List<String> roles = (rawRolesCsv == null || rawRolesCsv.isBlank())
                ? List.of()
                : Arrays.stream(rawRolesCsv.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .toList();

        return new CurrentUser(userId, email, roles);
    }
}
