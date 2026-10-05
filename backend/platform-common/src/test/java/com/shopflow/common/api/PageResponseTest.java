package com.shopflow.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    @DisplayName("of() menghitung totalPages dan penanda first/last dengan benar")
    void computesPaginationFlags() {
        PageResponse<String> firstPage = PageResponse.of(List.of("a", "b"), 0, 2, 5);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();

        PageResponse<String> lastPage = PageResponse.of(List.of("e"), 2, 2, 5);
        assertThat(lastPage.first()).isFalse();
        assertThat(lastPage.last()).isTrue();
    }

    @Test
    @DisplayName("halaman terakhir dari jumlah yang pas tetap ditandai last")
    void singlePageIsBothFirstAndLast() {
        PageResponse<String> only = PageResponse.of(List.of("a"), 0, 10, 1);

        assertThat(only.totalPages()).isEqualTo(1);
        assertThat(only.first()).isTrue();
        assertThat(only.last()).isTrue();
    }

    @Test
    @DisplayName("hasil kosong tidak memicu pembagian dengan nol")
    void handlesEmptyResult() {
        PageResponse<String> empty = PageResponse.of(List.of(), 0, 12, 0);

        assertThat(empty.content()).isEmpty();
        assertThat(empty.totalPages()).isZero();
    }
}
