package com.shopflow.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private static ShippingAddress address() {
        return new ShippingAddress("Budi", "08123", "Jl. Merdeka 1", null,
                "Bandung", "40115", "Indonesia");
    }

    private static Order orderWithItems() {
        Order order = new Order("ORD-20261005-ABC123", 42L, "budi@example.com", address(), "catatan");
        order.addItem(new OrderItem(1L, "KOP-001", "Kopi Gayo", null, 8_500_000L, 2));
        order.addItem(new OrderItem(2L, "TEH-001", "Teh Jasmine", null, 6_200_000L, 3));
        return order;
    }

    @Test
    @DisplayName("total dihitung ulang setiap kali item ditambahkan")
    void recalculatesTotalOnEveryAdd() {
        Order order = orderWithItems();

        assertThat(order.getTotalCents()).isEqualTo(8_500_000L * 2 + 6_200_000L * 3);
        assertThat(order.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("menambahkan item juga menetapkan relasi balik ke pesanan")
    void setsBackReference() {
        Order order = orderWithItems();

        assertThat(order.getItems()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(order));
    }

    @Test
    @DisplayName("itemCount menjumlahkan kuantitas")
    void countsTotalQuantity() {
        assertThat(orderWithItems().itemCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("pesanan baru selalu mulai dari PENDING_PAYMENT")
    void startsAsPendingPayment() {
        Order order = new Order("ORD-1", 1L, null, address(), null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getTotalCents()).isZero();
        assertThat(order.getCurrency()).isEqualTo("IDR");
    }

    @Test
    @DisplayName("transisi status terminal dikenali dengan benar")
    void knowsTerminalStatuses() {
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.FULFILLED.isTerminal()).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.isTerminal()).isFalse();
        assertThat(OrderStatus.PAID.isTerminal()).isFalse();
        assertThat(OrderStatus.PAYMENT_FAILED.isTerminal()).isFalse();
    }
}
