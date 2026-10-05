package com.shopflow.order.service;

/** Satu baris stok yang perlu ditahan / dilepas kembali. */
public record ReservedLine(Long productId, int quantity) {
}
