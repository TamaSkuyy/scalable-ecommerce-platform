package com.shopflow.cart.service;

import com.shopflow.cart.api.dto.AddItemRequest;
import com.shopflow.cart.api.dto.CartResponse;
import com.shopflow.cart.api.dto.UpdateItemRequest;
import com.shopflow.cart.client.CatalogGateway;
import com.shopflow.cart.client.dto.ProductSnapshot;
import com.shopflow.cart.domain.CartItem;
import com.shopflow.cart.domain.CartState;
import com.shopflow.cart.repository.CartRepository;
import com.shopflow.common.api.BadRequestException;
import com.shopflow.common.api.ConflictException;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.common.api.UnauthorizedException;
import com.shopflow.common.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    /** Batas wajar per produk: mencegah keranjang berisi 9999 unit lalu menahan stok. */
    static final int MAX_QUANTITY_PER_ITEM = 20;

    private final CartRepository cartRepository;
    private final CatalogGateway catalogGateway;

    public CartService(CartRepository cartRepository, CatalogGateway catalogGateway) {
        this.cartRepository = cartRepository;
        this.catalogGateway = catalogGateway;
    }

    public CartResponse view(Long userId) {
        return CartResponse.from(load(userId));
    }

    public CartResponse addItem(Long userId, AddItemRequest request) {
        ProductSnapshot product = catalogGateway.fetch(request.productId());
        if (!product.active()) {
            throw new ConflictException("Produk ini sudah tidak dijual");
        }

        CartState cart = load(userId);
        int alreadyInCart = cart.items().stream()
                .filter(item -> item.productId().equals(product.id()))
                .mapToInt(CartItem::quantity)
                .findFirst()
                .orElse(0);

        int target = alreadyInCart + request.quantity();
        validateQuantity(target, product);

        List<CartItem> items = new ArrayList<>(cart.items().stream()
                .filter(item -> !item.productId().equals(product.id()))
                .toList());
        items.add(new CartItem(
                product.id(),
                product.sku(),
                product.slug(),
                product.name(),
                product.imageUrl(),
                product.priceCents(),
                target));

        log.debug("User {} menambah produk {} -> qty {}", userId, product.id(), target);
        return CartResponse.from(save(userId, items));
    }

    public CartResponse updateItem(Long userId, Long productId, UpdateItemRequest request) {
        CartState cart = load(userId);
        CartItem existing = cart.items().stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Item keranjang", productId));

        // quantity = 0 berarti "hapus" - konvensi yang sama dipakai banyak API.
        if (request.quantity() == 0) {
            return removeItem(userId, productId);
        }

        ProductSnapshot product = catalogGateway.fetch(productId);
        validateQuantity(request.quantity(), product);

        List<CartItem> items = cart.items().stream()
                .map(item -> item.productId().equals(productId)
                        // Harga disegarkan dari katalog, bukan dibiarkan basi.
                        ? new CartItem(product.id(), product.sku(), product.slug(), product.name(),
                        product.imageUrl(), product.priceCents(), request.quantity())
                        : item)
                .toList();

        return CartResponse.from(save(userId, items));
    }

    public CartResponse removeItem(Long userId, Long productId) {
        CartState cart = load(userId);
        List<CartItem> remaining = cart.items().stream()
                .filter(item -> !item.productId().equals(productId))
                .toList();

        if (remaining.isEmpty()) {
            // Keranjang kosong tidak perlu disimpan - biar Redis bersih dan TTL tidak menumpuk.
            cartRepository.delete(userId);
            return CartResponse.from(CartState.empty(userId));
        }
        return CartResponse.from(save(userId, remaining));
    }

    public void clear(Long userId) {
        cartRepository.delete(userId);
        log.debug("Keranjang user {} dikosongkan", userId);
    }

    /** Dipakai order-service lewat endpoint internal. */
    public CartResponse viewByUserId(Long userId) {
        return view(userId);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private CartState load(Long userId) {
        return cartRepository.find(userId).orElseGet(() -> CartState.empty(userId));
    }

    private CartState save(Long userId, List<CartItem> items) {
        List<CartItem> sorted = items.stream()
                .sorted(Comparator.comparing(CartItem::productId))
                .toList();
        return cartRepository.save(new CartState(userId, sorted, Instant.now()));
    }

    private void validateQuantity(int quantity, ProductSnapshot product) {
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException(
                    "Maksimal " + MAX_QUANTITY_PER_ITEM + " unit per produk dalam satu pesanan");
        }
        if (quantity > product.stockQuantity()) {
            throw new ConflictException("Stok " + product.name() + " hanya tersisa " + product.stockQuantity());
        }
    }

    /** Mengubah {@link CurrentUser} menjadi userId, atau melempar 401. */
    public static Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAuthenticated()) {
            throw new UnauthorizedException("Kamu harus login untuk menggunakan keranjang");
        }
        return currentUser.id();
    }
}
