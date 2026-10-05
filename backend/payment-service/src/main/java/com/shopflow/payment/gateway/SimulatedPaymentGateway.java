package com.shopflow.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulasi payment gateway.
 *
 * <p><b>Ini satu-satunya kelas yang perlu diganti</b> saat kamu memakai Stripe/PayPal.
 * Kontraknya sengaja sederhana: charge -> sukses (dengan referensi) atau gagal (dengan alasan).
 *
 * <p>Contoh integrasi nyata (Stripe):
 * <pre>
 * PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
 *     .setAmount(amountCents)
 *     .setCurrency(currency.toLowerCase())
 *     .setIdempotencyKey(paymentId)   // <-- kunci anti double-charge
 *     .build();
 * PaymentIntent intent = PaymentIntent.create(params);
 * </pre>
 *
 * <p>Atur tingkat keberhasilan lewat env var {@code SHOPFLOW_PAYMENT_SUCCESS_RATE}:
 * {@code 1.0} selalu sukses, {@code 0.0} selalu gagal (berguna untuk menguji alur gagal).
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    private final double successRate;

    public SimulatedPaymentGateway(
            @Value("${shopflow.payment.simulated-success-rate:0.9}") double successRate) {
        this.successRate = Math.clamp(successRate, 0.0, 1.0);
    }

    @Override
    public ChargeResult charge(String paymentId, long amountCents, String currency) {
        // Di dunia nyata, di sinilah panggilan HTTPS ke Stripe/PayPal terjadi.
        log.info("[SIMULASI] Menghubungi payment gateway: paymentId={} amount={} {}",
                paymentId, amountCents, currency);

        boolean approved = ThreadLocalRandom.current().nextDouble() < successRate;
        if (approved) {
            return ChargeResult.approved("ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        return ChargeResult.declined(declineReason());
    }

    private String declineReason() {
        String[] reasons = {
                "Kartu ditolak oleh bank penerbit (insufficient funds)",
                "Kartu kedaluwarsa",
                "Transaksi dicurigai fraud oleh sistem risiko",
                "3D Secure tidak diselesaikan"
        };
        return reasons[ThreadLocalRandom.current().nextInt(reasons.length)];
    }
}
