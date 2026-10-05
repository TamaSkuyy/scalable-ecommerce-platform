package com.shopflow.cart.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Isi keranjang yang disimpan sebagai satu nilai JSON di Redis.
 *
 * <p>Kenapa satu key per user, bukan satu hash per item?
 * Karena keranjang selalu dibaca dan ditulis sebagai satu kesatuan. Satu key =
 * satu round-trip, dan operasinya atomik tanpa perlu transaksi Redis.
 */
public record CartState(Long userId, List<CartItem> items, Instant updatedAt) implements Serializable {

    public CartState {
        items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public static CartState empty(Long userId) {
        return new CartState(userId, new ArrayList<>(), Instant.now());
    }

    public CartState touch() {
        return new CartState(userId, items, Instant.now());
    }

    public long subtotalCents() {
        return items.stream().mapToLong(CartItem::lineTotalCents).sum();
    }

    public int itemCount() {
        return items.stream().mapToInt(CartItem::quantity).sum();
    }

    public String currency() {
        return "IDR";
    }
}
