package com.shopflow.payment.gateway;

/**
 * Abstraksi payment gateway.
 *
 * <p>Kenapa perlu interface? Supaya kode bisnis tidak bergantung pada Stripe/PayPal.
 * Efeknya: alur pembayaran bisa di-unit-test dengan gateway palsu, dan mengganti
 * penyedia pembayaran tidak menyentuh {@code PaymentService}.
 */
public interface PaymentGateway {

    ChargeResult charge(String paymentId, long amountCents, String currency);

    record ChargeResult(boolean success, String providerReference, String failureReason) {

        public static ChargeResult approved(String providerReference) {
            return new ChargeResult(true, providerReference, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}
