package com.shopflow.product.api;

import com.shopflow.product.api.dto.ProductSnapshotResponse;
import com.shopflow.product.api.dto.StockOperationRequest;
import com.shopflow.product.api.dto.StockResponse;
import com.shopflow.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API internal untuk service lain. Dilindungi {@code InternalApiTokenFilter}
 * (header {@code X-Internal-Token}), TIDAK diekspos lewat API Gateway.
 */
@RestController
@RequestMapping("/api/v1/internal/products")
@Tag(name = "Internal - Products", description = "Dipakai cart-service & order-service")
public class InternalProductController {

    private final ProductService productService;

    public InternalProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Snapshot produk (harga, stok, status aktif)")
    public ProductSnapshotResponse snapshot(@PathVariable Long id) {
        return productService.findSnapshot(id);
    }

    @PostMapping("/{id}/stock/reserve")
    @Operation(summary = "Tahan stok saat checkout")
    public StockResponse reserve(@PathVariable Long id, @Valid @RequestBody StockOperationRequest request) {
        return productService.adjustStock(id, -request.quantity(), "reserve " + request.reference());
    }

    @PostMapping("/{id}/stock/release")
    @Operation(summary = "Lepas stok (pembatalan / pembayaran gagal)")
    public StockResponse release(@PathVariable Long id, @Valid @RequestBody StockOperationRequest request) {
        return productService.adjustStock(id, request.quantity(), "release " + request.reference());
    }
}
