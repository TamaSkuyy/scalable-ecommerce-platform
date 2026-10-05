package com.shopflow.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ini adalah contoh betapa enaknya menguji kode yang tidak bergantung framework.
 *
 * <p>Tidak ada Spring context, tidak ada database, tidak ada mock - hanya logika murni.
 * Test seperti ini jalan dalam milidetik dan tidak pernah "flaky".
 */
class CurrentUserTest {

    @Test
    @DisplayName("header lengkap dipetakan menjadi user terautentikasi")
    void mapsCompleteHeaders() {
        CurrentUser user = CurrentUser.of("42", "Budi@Example.com", "CUSTOMER,ADMIN");

        assertThat(user.isAuthenticated()).isTrue();
        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.email()).isEqualTo("Budi@Example.com");
        assertThat(user.roles()).containsExactly("CUSTOMER", "ADMIN");
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("role dipangkas dan entri kosong dibuang")
    void trimsRoles() {
        CurrentUser user = CurrentUser.of("1", "a@b.c", " CUSTOMER , ,ADMIN ");

        assertThat(user.roles()).containsExactly("CUSTOMER", "ADMIN");
    }

    @Test
    @DisplayName("tanpa header -> user anonim, bukan error")
    void anonymousWhenHeadersMissing() {
        CurrentUser user = CurrentUser.of(null, null, null);

        assertThat(user.isAuthenticated()).isFalse();
        assertThat(user.id()).isNull();
        assertThat(user.roles()).isEmpty();
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("X-User-Id yang rusak tidak boleh menghasilkan 500")
    void malformedIdBecomesAnonymous() {
        CurrentUser user = CurrentUser.of("bukan-angka", "a@b.c", "CUSTOMER");

        assertThat(user.isAuthenticated()).isFalse();
        assertThat(user.id()).isNull();
        // Header email tetap dipakai supaya log tetap berguna.
        assertThat(user.email()).isEqualTo("a@b.c");
    }

    @Test
    @DisplayName("anonymous constant aman dipakai berulang")
    void anonymousConstantIsEmpty() {
        assertThat(CurrentUser.ANONYMOUS.isAuthenticated()).isFalse();
        assertThat(CurrentUser.ANONYMOUS.roles()).isEqualTo(List.of());
    }
}
