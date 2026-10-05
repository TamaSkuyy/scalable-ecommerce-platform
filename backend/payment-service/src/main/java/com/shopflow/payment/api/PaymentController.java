package com.shopflow.payment.api;

import com.shopflow.common.api.PageResponse;
import com.shopflow.common.api.UnauthorizedException;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.payment.api.dto.PaymentResponse;
import com.shopflow.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Status pembayaran")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Status pembayaran sebuah pesanan")
    public PaymentResponse byOrderNumber(CurrentUser currentUser, @PathVariable String orderNumber) {
        return paymentService.findByOrderNumber(orderNumber, requireUser(currentUser).id());
    }

    @GetMapping
    @Operation(summary = "Riwayat pembayaran milik user yang login")
    public PageResponse<PaymentResponse> list(CurrentUser currentUser,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return paymentService.listForUser(requireUser(currentUser).id(),
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAuthenticated()) {
            throw new UnauthorizedException("Kamu harus login untuk melihat pembayaran");
        }
        return currentUser;
    }
}
