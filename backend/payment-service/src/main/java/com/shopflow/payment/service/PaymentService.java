package com.shopflow.payment.service;

import com.shopflow.common.api.ForbiddenException;
import com.shopflow.common.api.PageResponse;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.payment.api.dto.PaymentResponse;
import com.shopflow.payment.domain.Payment;
import com.shopflow.payment.domain.PaymentRepository;
import com.shopflow.payment.event.PaymentResultDomainEvent;
import com.shopflow.payment.gateway.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher events;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          ApplicationEventPublisher events) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.events = events;
    }

    /**
     * Memproses pembayaran untuk sebuah pesanan.
     *
     * <p><b>Idempotensi.</b> Pengecekan {@code existsByOrderNumber} + unique constraint di
     * database memastikan pesan yang terkirim dua kali tidak menghasilkan dua kali charge.
     * Ini WAJIB ada di setiap konsumen event: RabbitMQ menjamin <i>at-least-once</i>,
     * bukan <i>exactly-once</i>.
     *
     * <p><b>Yang masih perlu dilengkapi di produksi:</b> panggilan ke payment gateway adalah
     * efek samping eksternal yang tidak bisa di-rollback. Kalau proses mati tepat setelah
     * gateway menyetujui tapi sebelum commit, transaksi bisa hilang dari database.
     * Solusinya: kirim {@code idempotency-key} ke gateway + pola Transactional Outbox.
     */
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderNumber(event.orderNumber())) {
            log.info("Pembayaran untuk pesanan {} sudah tercatat - event duplikat diabaikan",
                    event.orderNumber());
            return;
        }

        String paymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase(Locale.ROOT);

        Payment payment = new Payment(
                paymentId,
                event.orderNumber(),
                event.userId(),
                event.totalCents(),
                event.currency(),
                "SIMULATED_CARD");
        paymentRepository.save(payment);

        PaymentGateway.ChargeResult result =
                paymentGateway.charge(paymentId, event.totalCents(), event.currency());

        if (result.success()) {
            payment.markSucceeded(result.providerReference());
            log.info("Pembayaran {} BERHASIL untuk pesanan {}", paymentId, event.orderNumber());
        } else {
            payment.markFailed(result.failureReason());
            log.warn("Pembayaran {} GAGAL untuk pesanan {}: {}", paymentId, event.orderNumber(),
                    result.failureReason());
        }

        Payment saved = paymentRepository.save(payment);

        // Diterbitkan di dalam transaksi; listener AFTER_COMMIT yang mengirim ke RabbitMQ.
        events.publishEvent(PaymentResultDomainEvent.from(saved, event.userId()));
    }

    @Transactional(readOnly = true)
    public PaymentResponse findByOrderNumber(String orderNumber, Long userId) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> ResourceNotFoundException.by("Pembayaran", "nomor pesanan", orderNumber));

        if (userId != null && payment.getUserId() != null && !payment.getUserId().equals(userId)) {
            throw new ForbiddenException("Pembayaran ini bukan milikmu");
        }
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listForUser(Long userId, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, PaymentResponse::from);
    }
}
