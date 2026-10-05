# Event-Driven: RabbitMQ

Dokumen ini menjelaskan komunikasi asinkron antar-service: topologi, kontrak pesan,
idempotensi, penanganan kegagalan, dan pola yang harus kamu tambahkan sebelum produksi.

---

## 1. Kapan memakai event, kapan memakai HTTP?

| Pertanyaan | Kalau jawabannya… |
|---|---|
| Kalau langkah ini gagal, apakah seluruh operasi harus dibatalkan? | **Ya → HTTP (sinkron).** Contoh: menahan stok saat checkout |
| Apakah user harus menunggu hasilnya? | **Tidak → event.** Contoh: kirim email, proses pembayaran |
| Apakah data yang dibutuhkan harus 100% terkini? | **Ya → HTTP.** Contoh: harga & stok saat checkout |
| Apakah penambahan konsumen baru seharusnya tidak mengubah pengirim? | **Ya → event** |

**Kesalahan paling umum:** memakai event untuk hal yang butuh jawaban langsung, lalu
bingung kenapa user melihat keranjang "seperti tidak tersimpan" selama satu detik.

---

## 2. Topologi

```
                       exchange: shopflow.events   (topic, durable)
                       ┌───────────────────────────────────────────────┐
                       │  routing key           →  queue                │
                       ├───────────────────────────────────────────────┤
                       │  order.created         →  shopflow.payment.order-created
                       │                        →  shopflow.notification.events
                       │  order.cancelled       →  shopflow.notification.events
                       │  payment.completed     →  shopflow.order.payment-result
                       │                        →  shopflow.notification.events
                       │  payment.failed        →  shopflow.order.payment-result
                       │                        →  shopflow.notification.events
                       └───────────────────────────────────────────────┘

                       exchange: shopflow.events.dlx  (direct, durable)
                       └─ routing key "dead-letter" → queue: shopflow.events.dlq
```

Setiap queue dideklarasikan dengan `x-dead-letter-exchange`. Pesan yang gagal diproses
3× akan pindah ke `shopflow.events.dlq` — **tidak hilang dan tidak berputar selamanya**.

Cara memeriksa di UI: [http://localhost:15672](http://localhost:15672) → Queues.
Angka di kolom `Ready` untuk `shopflow.events.dlq` harus 0. Kalau tidak, ada bug.

---

## 3. Kontrak pesan

Semua event memakai bentuk amplop yang sama. Header membawa metadata supaya konsumen bisa
memilih handler **tanpa harus membuka payload**.

| Header | Isi |
|---|---|
| `x-event-type` | routing key, mis. `order.created` |
| `x-correlation-id` | id request yang memicu event ini (dari MDC) |
| `content_type` | `application/json` |

### `order.created` — order-service → payment, notification

```json
{
  "eventId": "3f9c1a7b-4e2d-4c05-9a11-8b2c4d5e6f70",
  "orderNumber": "ORD-20261005-7F3A91",
  "userId": 1,
  "userEmail": "budi@example.com",
  "totalCents": 17000000,
  "currency": "IDR",
  "lines": [
    { "productId": 1, "sku": "KOP-001", "name": "Kopi Gayo Arabica 250g",
      "imageUrl": "https://...", "unitPriceCents": 8500000, "quantity": 2 }
  ],
  "occurredAt": "2026-10-05T10:02:00Z"
}
```

### `payment.completed` — payment-service → order, notification

```json
{
  "eventId": "...",
  "orderNumber": "ORD-20261005-7F3A91",
  "userId": 1,
  "paymentId": "PAY-7F3A91C2",
  "providerRef": "ch_9a8b7c6d5e4f3a2b",
  "method": "SIMULATED_CARD",
  "amountCents": 17000000,
  "currency": "IDR",
  "occurredAt": "2026-10-05T10:02:01Z"
}
```

### `payment.failed`

Sama seperti di atas, tapi `providerRef` diganti `reason` (alasan penolakan).

### `order.cancelled` — order-service → notification

```json
{
  "eventId": "...",
  "orderNumber": "ORD-20261005-7F3A91",
  "userId": 1,
  "reason": "Dibatalkan oleh pengguna",
  "occurredAt": "2026-10-05T10:05:00Z"
}
```

Semua DTO ini ada di `platform-common/src/main/java/com/shopflow/common/events/`.
**Nama exchange, queue, dan routing key tidak boleh ditulis sebagai string literal di
service mana pun** — semuanya lewat `EventRouting` supaya typo tidak menyebabkan pesan
hilang tanpa error.

---

## 4. Alur lengkap satu checkout

```
t=0.00s  POST /api/v1/orders
t=0.05s  order-service: ambil keranjang (cart-service)
t=0.08s  order-service: ambil harga terkini (product-service)
t=0.12s  order-service: tahan stok 2 item (product-service)
t=0.15s  order-service: simpan pesanan  ── COMMIT ──► status PENDING_PAYMENT
         ▼ (setelah commit)
t=0.16s  order-service: kosongkan keranjang
t=0.17s  order-service: publish `order.created`
t=0.17s  ─────► HTTP 201 dikembalikan ke user ◄─────

         ... user sudah melihat halaman pesanan, pekerjaan latar belakang berjalan:

t=0.20s  payment-service: terima `order.created`
t=0.20s  payment-service: simpan Payment(PENDING)  ── COMMIT
t=0.22s  payment-service: charge ke gateway (simulasi 50–200ms)
t=0.25s  payment-service: update Payment(SUCCEEDED) ── COMMIT
         ▼ (setelah commit)
t=0.26s  payment-service: publish `payment.completed`

t=0.28s  order-service:  terima → status PAID
t=0.28s  notification-service: terima → "EMAIL TERKIRIM (SIMULASI)" di log

t≈1–2s   frontend melakukan polling / refresh → user melihat "Sudah Dibayar"
```

Perhatikan: **langkah 1–6 selesai dalam ~170 ms**, sementara pembayaran berjalan di
belakang. Inilah alasan utama memakai event.

---

## 5. Idempotensi — bagian yang wajib dipahami

RabbitMQ menjamin **at-least-once delivery**, bukan exactly-once. Artinya pesan
**akan** terkirim dua kali pada suatu saat (network hiccup, consumer restart, retry).
Kode kamu harus siap.

Tiga lapis pertahanan yang dipakai di proyek ini:

| Lapis | Tempat | Cara kerja |
|---|---|---|
| 1. Cek sebelum bertindak | `PaymentService.handleOrderCreated` | `existsByOrderNumber(...)` → kalau ada, keluar |
| 2. Unique constraint | tabel `payments.order_number`, `notifications.event_id` | Percobaan kedua ditolak database, walau pengecekan di lapis 1 terlewat |
| 3. Transisi status yang aman | `OrderStore.markPaid` / `markPaymentFailed` | Hanya bertransisi dari `PENDING_PAYMENT`; kembali `empty()` kalau tidak ada perubahan, sehingga kompensasi tidak dijalankan dua kali |

**Lapis 3 adalah yang paling sering dilupakan.** Perhatikan `markPaymentFailed`
mengembalikan `Optional<Order>`: kalau kosong, berarti tidak ada perubahan, jadi
pelepasan stok **tidak** dijalankan. Tanpa itu, event duplikat akan melepas stok dua kali.

---

## 6. Trusted packages — jebakan pertama yang akan kamu temui

Kalau kamu menambah event baru dan tiba-tiba konsumen menolak pesan dengan error seperti ini:

```
ListenerExecutionFailedException: Failed to convert message
Caused by: IllegalArgumentException: The class 'com.shopflow.common.events.PaymentCompletedEvent'
           is not in the trusted packages: [java.util, java.lang].
```

…itu **bukan bug di kodemu**, melainkan proteksi yang bekerja.

**Kenapa ada.** `Jackson2JsonMessageConverter` menulis header `__TypeId__` berisi nama kelas
payload, lalu memakai header itu untuk menentukan kelas tujuan saat menerima. Header itu
datang dari broker — artinya siapa pun yang bisa menulis ke exchange (atau menyusup lewat
satu konsumen) dapat menitipkan `__TypeId__` berisi kelas apa pun. Kalau aplikasi menerima
semuanya, itu menjadi <i>deserialization gadget chain</i>: kelas dari library di classpath
bisa dipakai untuk mengeksekusi kode.

**Jangan** menyelesaikannya dengan `setTrustedPackages("*")`. Itu mematikan seluruh proteksi
dan justru masalah keamanan yang nyata, bukan sekadar formalitas.

**Cara yang benar** — tuliskan paket eventmu secara eksplisit. Di proyek ini hal itu
dilakukan sekali di
`platform-common/.../events/RabbitEventSupportAutoConfiguration.java`:

```java
public static final String[] TRUSTED_PACKAGES = {
        "com.shopflow.common.events",
        "java.util",
        "java.lang"
};

@Bean
@ConditionalOnMissingBean(MessageConverter.class)
public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
    DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
    typeMapper.setTrustedPackages(TRUSTED_PACKAGES);
    converter.setJavaTypeMapper(typeMapper);
    return converter;
}
```

**Yang perlu kamu ingat saat menambah event baru:** kalau record event-mu **tidak** berada
di paket `com.shopflow.common.events`, kamu harus menambahkan paketnya ke daftar itu.
Gejalanya persis seperti di atas, dan pesan errornya sudah memberitahu kelas mana yang ditolak.

### Catatan: konversi payload selalu dijalankan

Menarik untuk dipahami — perhatikan `MessagingMessageListenerAdapter.onMessage()`:

```java
message = toMessagingMessage(amqpMessage);      // <- payload SELALU dikonversi
invokeHandlerAndProcessResult(amqpMessage, channel, message);
```

Bahkan ketika metode listener-mu bertipe `Message` dan melakukan parsing sendiri
(`objectMapper.readValue(message.getBody(), ...)`), konversi tetap terjadi lebih dulu —
karena Spring AMQP menyiapkan `Message<?>` sebelum memanggil handler. Kalau konversi gagal,
handler-mu tidak akan pernah dipanggil.

Kabar baiknya: parameter bertipe `Message` tetap menerima objek mentah (dilewatkan sebagai
*provided argument*), jadi parsing manual di dalam handler tetap bekerja seperti biasa.

Karena itu, listener di proyek ini sengaja memakai pola:
**konversi payload tidak dipakai; dispatch dilakukan dari header `x-event-type`.**

---

## 7. Penanganan kegagalan

### Di sisi konsumen

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: auto
        default-requeue-rejected: false     # penting!
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1s
          multiplier: 2
```

Alurnya:

```
exception  ──►  retry 1 (1s)  ──►  retry 2 (2s)  ──►  retry 3 (4s)  ──►  DLQ
```

**Kalau `default-requeue-rejected: true`** (nilai bawaan), pesan akan dikembalikan ke
queue tanpa henti dan memakan CPU. Ini penyebab nomor satu "RabbitMQ saya penuh".

### Di sisi produsen

`spring.rabbitmq.publisher-confirm-type: correlated` sudah aktif. Untuk benar-benar
yakin pesan sampai, tangani `ConfirmCallback`:

```java
rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) {
        log.error("Pesan TIDAK sampai ke broker: {}", cause);
        // -> simpan ke tabel outbox untuk dikirim ulang
    }
});
```

### Memeriksa isi DLQ

```bash
# Lihat pesan yang gagal (tanpa menghapusnya)
docker compose exec rabbitmq rabbitmqadmin \
  --username shopflow --password shopflow \
  get queue=shopflow.events.dlq count=5 ackmode=ack_requeue_true
```

Setelah bug diperbaiki, kirim ulang ke exchange asal:

```bash
docker compose exec rabbitmq rabbitmqadmin \
  --username shopflow --password shopflow \
  publish exchange=shopflow.events routing_key=order.created \
  payload='{"eventId":"manual-1", ...}'
```

---

## 8. Masalah dual-write & Transactional Outbox {#outbox}

**Masalahnya.** Dalam `OrderStore.create()` kita melakukan dua hal berbeda:

1. `COMMIT` ke PostgreSQL (pesanan tersimpan)
2. Publish ke RabbitMQ

Dua sistem berbeda, tidak ada transaksi bersama. Kemungkinan kegagalan:

| Mati di antara… | Akibat |
|---|---|
| sebelum COMMIT | aman — tidak ada pesanan, tidak ada event |
| **setelah COMMIT, sebelum publish** | **pesanan ada, tapi pembayaran tidak pernah diproses** |

`@TransactionalEventListener(AFTER_COMMIT)` menghilangkan masalah "event hantu"
(event terkirim padahal transaksi di-rollback), tapi **tidak** menyelesaikan kasus
aplikasi mati tepat setelah commit.

**Solusinya: Transactional Outbox.**

```
BEGIN;
  INSERT INTO orders (...) VALUES (...);
  INSERT INTO outbox (id, topic, payload, created_at) VALUES (...);   -- satu transaksi
COMMIT;                                                              -- keduanya atau tidak sama sekali

-- proses terpisah (scheduler / CDC Debezium):
SELECT * FROM outbox WHERE published_at IS NULL ORDER BY id LIMIT 100;
  → publish ke RabbitMQ
  → UPDATE outbox SET published_at = now() WHERE id = ...;
```

Sekarang "aplikasi mati setelah commit" hanya berarti event akan terkirim saat
aplikasi hidup lagi. Tidak ada yang hilang.

**Latihan:** implementasikan tabel `outbox` di order-service, ganti
`OrderPostCommitListener` dengan poller `@Scheduled`, dan buat test yang membuktikan
event dikirim tepat satu kali walau poller berjalan dua kali.

---

## 9. Menambah event atau konsumen baru

### Menambah event baru (mis. `order.shipped`)

1. Tambahkan konstanta routing key di `EventRouting`
2. Buat record DTO di `com.shopflow.common.events`
3. Tambahkan method `publishOrderShipped(...)` di `OrderEventPublisher`
4. Di setiap konsumen: deklarasikan `Queue` + `Binding`, dan tambahkan `case` di listener

### Menambah konsumen baru (mis. `analytics-service`)

Penerbit **tidak perlu diubah sama sekali**. Cukup:

1. Buat service baru dengan `spring-boot-starter-amqp`
2. Deklarasikan exchange + queue sendiri (idempoten, aman walau sudah ada)
3. `@RabbitListener(queues = "shopflow.analytics.events")`

Inilah keuntungan utama event-driven: **penambahan konsumen nol dampak ke pengirim.**

---

## 10. Mengganti RabbitMQ dengan Kafka

Kalau nanti butuh *event replay*, retensi jangka panjang, atau throughput sangat tinggi,
Kafka lebih tepat. Yang perlu berubah:

| Aspek | RabbitMQ (sekarang) | Kafka |
|---|---|---|
| Model | smart broker, dumb consumer | dumb broker, smart consumer |
| Pesan setelah dibaca | hilang (kecuali di-ack dan retry) | tetap ada sampai `retention.ms` |
| Replay | tidak bisa | bisa (reset offset) |
| Unit paralelisme | jumlah consumer per queue | jumlah partisi |
| Dependency | `spring-boot-starter-amqp` | `spring-kafka` |
| Anotasi listener | `@RabbitListener(queues=…)` | `@KafkaListener(topics=…)` |

Karena `EventRouting` dan DTO event sudah terpusat di `platform-common`, migrasinya
terbatas pada kelas `*Listener` dan `*EventPublisher` per service. Logika bisnis
(`OrderService`, `PaymentService`) tidak perlu berubah — inilah manfaat memisahkan
"event domain" dari "transport event".

---

## 11. Debugging cepat

```bash
# Semua event yang pernah diproses order-service
docker compose logs order-service | grep "Menerima event"

# Satu request lintas service (pakai correlation id dari log gateway)
CID=$(docker compose logs api-gateway | grep -o '"x-correlation-id":"[^"]*"' | tail -1 | cut -d'"' -f4)
docker compose logs | grep "$CID"

# Berapa pesan yang menunggu di setiap queue
docker compose exec rabbitmq rabbitmqctl list_queues name messages consumers

# Isi dead-letter queue
docker compose exec rabbitmq rabbitmqctl list_queues name messages | grep dlq
```
