package com.shopflow.notification.service;

import com.shopflow.common.api.ForbiddenException;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.notification.api.dto.NotificationResponse;
import com.shopflow.notification.domain.Notification;
import com.shopflow.notification.domain.NotificationRepository;
import com.shopflow.notification.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Membuat isi notifikasi dan "mengirimnya".
 *
 * <p>Semua pesan ditulis dalam Bahasa Indonesia dan memakai format Rupiah yang benar
 * ({@code Intl}/${@code NumberFormat}) - bukan menyambung string manual, yang selalu
 * berakhir dengan "Rp1500000.0".
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final Locale INDONESIA = Locale.forLanguageTag("id-ID");

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------
    // Handler per jenis event
    // ------------------------------------------------------------------

    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        deliver(event.eventId(), event.userId(), NotificationType.ORDER_CREATED,
                "Pesanan " + event.orderNumber() + " berhasil dibuat",
                "Terima kasih! Pesanan kamu senilai " + formatRupiah(event.totalCents(), event.currency())
                        + " sudah kami terima dengan nomor " + event.orderNumber()
                        + ". Kami sedang memproses pembayaran.",
                event.orderNumber());
    }

    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        deliver(event.eventId(), event.userId(), NotificationType.ORDER_CANCELLED,
                "Pesanan " + event.orderNumber() + " dibatalkan",
                "Pesanan " + event.orderNumber() + " telah dibatalkan. Alasan: "
                        + (event.reason() == null ? "-" : event.reason())
                        + ". Stok yang sempat ditahan sudah kami lepas kembali.",
                event.orderNumber());
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        deliver(event.eventId(), event.userId(), NotificationType.PAYMENT_COMPLETED,
                "Pembayaran untuk " + event.orderNumber() + " berhasil",
                "Pembayaran sebesar " + formatRupiah(event.amountCents(), event.currency())
                        + " berhasil kami terima. Pesanan kamu sedang kami siapkan untuk pengiriman. "
                        + "Referensi pembayaran: " + event.paymentId() + ".",
                event.orderNumber());
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        deliver(event.eventId(), event.userId(), NotificationType.PAYMENT_FAILED,
                "Pembayaran untuk " + event.orderNumber() + " gagal",
                "Kami tidak dapat memproses pembayaran sebesar "
                        + formatRupiah(event.amountCents(), event.currency())
                        + ". Alasan: " + (event.reason() == null ? "-" : event.reason())
                        + ". Silakan coba lagi dengan metode pembayaran lain.",
                event.orderNumber());
    }

    // ------------------------------------------------------------------
    // Baca
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Notifikasi", id));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Notifikasi ini bukan milikmu");
        }
        notification.markRead();
        repository.save(notification);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    /**
     * Menyimpan + "mengirim" notifikasi.
     *
     * <p>{@code existsByEventId} menegakkan idempotensi: event yang sama (judulnya punya
     * {@code eventId} unik dari produsen) hanya menghasilkan satu notifikasi walau
     * RabbitMQ mengirimnya beberapa kali.
     */
    private void deliver(String eventId, Long userId, NotificationType type,
                         String subject, String body, String orderNumber) {

        if (userId == null) {
            log.warn("Event {} tidak punya userId - notifikasi dilewati", eventId);
            return;
        }

        if (repository.existsByEventId(eventId)) {
            log.info("Notifikasi untuk event {} sudah ada - duplikat diabaikan", eventId);
            return;
        }

        repository.save(new Notification(eventId, userId, type, subject, body, orderNumber));

        // ------------------------------------------------------------------
        // Di produksi, ganti blok ini dengan SendGrid / Twilio / SMTP.
        //     sendGrid.send(Mail.builder().to(userEmail).subject(subject).content(body).build());
        // ------------------------------------------------------------------
        log.info("""

                ============================ EMAIL TERKIRIM (SIMULASI) ============================
                Kepada : user#%d
                Subjek : %s
                Isi    : %s
                ==================================================================================
                """.formatted(userId, subject, body));
    }

    private String formatRupiah(long amountCents, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(INDONESIA);
        format.setCurrency(java.util.Currency.getInstance(currency == null ? "IDR" : currency));
        format.setMaximumFractionDigits(0);
        return format.format(BigDecimal.valueOf(amountCents, 2));
    }
}
