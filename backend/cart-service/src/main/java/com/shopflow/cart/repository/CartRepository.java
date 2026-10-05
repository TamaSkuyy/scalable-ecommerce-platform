package com.shopflow.cart.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.cart.domain.CartState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Akses Redis untuk keranjang.
 *
 * <p>Kita memilih {@code StringRedisTemplate} + JSON, bukan serializer biner Java.
 * Alasannya: nilai di Redis jadi bisa dibaca manusia ({@code redis-cli GET ...}),
 * tidak terikat versi class Java, dan bisa dipakai service lain saat debugging.
 */
@Repository
public class CartRepository {

    private static final Logger log = LoggerFactory.getLogger(CartRepository.class);

    /** Keranjang yang tidak disentuh selama 30 hari dianggap ditinggalkan. */
    private static final Duration CART_TTL = Duration.ofDays(30);
    private static final String KEY_PREFIX = "shopflow:cart:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CartRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Optional<CartState> find(Long userId) {
        String json = redis.opsForValue().get(key(userId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CartState.class));
        } catch (JsonProcessingException e) {
            // Data tidak bisa dibaca = tidak ada gunanya dipertahankan. Log lalu mulai ulang.
            log.error("Keranjang user {} rusak, dihapus. Penyebab: {}", userId, e.getMessage());
            delete(userId);
            return Optional.empty();
        }
    }

    public CartState save(CartState cart) {
        try {
            redis.opsForValue().set(key(cart.userId()), objectMapper.writeValueAsString(cart), CART_TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Gagal menyimpan keranjang user " + cart.userId(), e);
        }
        return cart;
    }

    public void delete(Long userId) {
        redis.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
