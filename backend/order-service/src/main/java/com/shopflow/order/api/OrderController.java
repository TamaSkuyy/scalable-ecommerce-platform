package com.shopflow.order.api;

import com.shopflow.common.api.PageResponse;
import com.shopflow.common.api.UnauthorizedException;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.order.api.dto.CreateOrderRequest;
import com.shopflow.order.api.dto.OrderDetailResponse;
import com.shopflow.order.api.dto.OrderSummaryResponse;
import com.shopflow.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Checkout dan riwayat pesanan")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Checkout: ubah isi keranjang menjadi pesanan")
    public OrderDetailResponse checkout(CurrentUser currentUser, @Valid @RequestBody CreateOrderRequest request) {
        CurrentUser user = requireUser(currentUser);
        return orderService.checkout(user.id(), user.email(), request);
    }

    @GetMapping
    @Operation(summary = "Riwayat pesanan milik user yang login")
    public PageResponse<OrderSummaryResponse> list(CurrentUser currentUser,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return orderService.listForUser(requireUser(currentUser).id(), page, size);
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Detail satu pesanan")
    public OrderDetailResponse detail(CurrentUser currentUser, @PathVariable String orderNumber) {
        return orderService.detail(orderNumber, requireUser(currentUser).id());
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Batalkan pesanan (hanya saat masih menunggu pembayaran)")
    public OrderDetailResponse cancel(CurrentUser currentUser, @PathVariable String orderNumber) {
        return orderService.cancel(orderNumber, requireUser(currentUser).id());
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAuthenticated()) {
            throw new UnauthorizedException("Kamu harus login untuk mengakses pesanan");
        }
        return currentUser;
    }
}
