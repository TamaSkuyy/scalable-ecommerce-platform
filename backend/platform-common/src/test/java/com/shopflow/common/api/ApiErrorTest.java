package com.shopflow.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    @Test
    @DisplayName("violations selalu berupa list, tidak pernah null")
    void violationsNeverNull() {
        ApiError error = ApiError.of(400, "Bad Request", "tidak valid", "/api/v1/x", "abc123", null);

        assertThat(error.violations()).isNotNull().isEmpty();
        assertThat(error.traceId()).isEqualTo("abc123");
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("daftar violations tidak bisa diubah dari luar")
    void violationsAreImmutable() {
        ApiError error = ApiError.of(400, "Bad Request", "tidak valid", "/api/v1/x", "t",
                List.of(new ApiError.FieldViolation("email", "wajib diisi")));

        assertThat(error.violations()).hasSize(1);
        assertThat(error.violations().get(0).field()).isEqualTo("email");
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> error.violations().add(new ApiError.FieldViolation("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("exception domain membawa status HTTP yang benar")
    void exceptionsCarryCorrectStatus() {
        assertThat(new ResourceNotFoundException("x").status().value()).isEqualTo(404);
        assertThat(new ConflictException("x").status().value()).isEqualTo(409);
        assertThat(new BadRequestException("x").status().value()).isEqualTo(400);
        assertThat(new ForbiddenException("x").status().value()).isEqualTo(403);
        assertThat(new UnauthorizedException("x").status().value()).isEqualTo(401);
        assertThat(new ServiceUnavailableException("x").status().value()).isEqualTo(503);
    }
}
