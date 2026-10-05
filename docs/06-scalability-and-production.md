# Skalabilitas & Menuju Produksi

Dokumen ini menjawab: *"base ini jalan di laptop saya. Bagaimana kalau ada 100.000 user?"*

Jawaban singkat: **ukur dulu, jangan tebak.** Tapi ada beberapa hal yang pasti menjadi
bottleneck lebih dulu, dan dokumen ini menunjukkan di mana titiknya di kode kita.

---

## 1. Yang menskalakan dengan mudah

| Komponen | Cara menskalakan | Kenapa mudah |
|---|---|---|
| Semua service backend | Tambah replika (`docker compose up --scale product-service=3`) | Stateless — tidak ada sesi di memori |
| API Gateway | Tambah replika + load balancer di depan | Stateless, hanya routing |
| Product Service (baca) | Tambah replika | Cache lokal per instance + query terindeks |
| Notification Service | Tambah replika | Murni konsumen event, urutan tidak penting |
| Discovery Service | Replikasi cluster Eureka | Saling mendaftar |

**Coba sekarang:**

```bash
docker compose up -d --scale product-service=3
# Buka Eureka: PRODUCT-SERVICE akan muncul dengan 3 instance
# Gateway otomatis round-robin ke ketiganya
for i in $(seq 1 6); do curl -s localhost:8080/api/v1/products?size=1 >/dev/null; done
docker compose logs product-service | grep -c "Menerima request"
```

Tidak ada satu pun konfigurasi yang perlu diubah. **Itulah alasan memakai `lb://` sejak awal.**

---

## 2. Yang TIDAK mudah — dan cara mengatasinya

### 2.1 Database transaksional

Ini bottleneck pertama dan paling sering diabaikan.

| Gejala | Perbaikan |
|---|---|
| Query lambat | Tambah indeks. Lihat `EXPLAIN ANALYZE`, jangan menebak |
| Connection pool habis | Turunkan waktu transaksi (jangan panggil HTTP di dalam transaksi — lihat `OrderStore`); naikkan pool dengan hati-hati (`(core_count * 2) + effective_spindle_count`) |
| Trafik tulis tinggi | Read replica untuk baca; partisi tabel besar (mis. `orders` per tahun) |
| Butuh skala horizontal | Sharding berdasarkan `user_id` — **hindari sampai benar-benar perlu**, biayanya besar |

**Sudah dilakukan di base ini:**
- Indeks eksplisit: `idx_orders_user_created`, `idx_products_category`, dst.
- `join fetch` untuk menghindari N+1 (bottleneck database paling umum).
- Transaksi pendek; panggilan jaringan di luar transaksi (`OrderService` vs `OrderStore`).
- Cache untuk data yang hampir statis (produk & kategori).

**Latihan ukur:**
```bash
docker compose exec postgres psql -U shopflow -d shopflow_product -c \
  "EXPLAIN ANALYZE SELECT * FROM products p JOIN categories c ON c.id = p.category_id
   WHERE p.active AND p.name ILIKE '%kopi%' LIMIT 12;"
```
Ganti `%kopi%` dengan `ILIKB '%kop%'` dan lihat perbedaan planner. Lalu coba dengan
indeks trigram: `CREATE INDEX ON products USING gin (name gin_trgm_ops);`
(ekstensi `pg_trgm` sudah diaktifkan di `infra/postgres/init/01-create-databases.sql`).

### 2.2 Sesi pengguna & rate limiting

Base ini stateless (JWT), jadi tidak ada state sesi. Yang perlu ditambahkan:

```yaml
# api-gateway: rate limiting berbasis Redis
spring:
  cloud:
    gateway:
      routes:
        - id: login-rate-limit
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/login
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 5
                redis-rate-limiter.burstCapacity: 10
                key-resolver: "#{@ipKeyResolver}"
```

Ini **wajib** untuk endpoint login/register, kalau tidak akun user bisa di-brute-force.

### 2.3 Keranjang di Redis

Redis satu instance = single point of failure. Untuk produksi:
- **Redis Sentinel** (failover otomatis) atau **Redis Cluster** (sharding)
- Pisahkan instance cache dari instance data yang harus dipertahankan
- Perhatikan `maxmemory-policy`: kita memakai `allkeys-lru` (benar untuk cache,
  **salah** kalau Redis juga menyimpan data yang tidak boleh hilang)

### 2.4 RabbitMQ

| Masalah | Perbaikan |
|---|---|
| Satu queue jadi bottleneck | Pecah berdasarkan partisi logis (mis. `order.created.user-{shard}`) — tapi ini menghilangkan jaminan urutan |
| Butuh urutan | Satu queue + satu consumer; atau *consistent hash exchange* |
| Pesan menumpuk saat consumer mati | Auto-scaling consumer berdasarkan `queue depth` (KEDA di Kubernetes) |
| Butuh replay | Pindah ke Kafka (lihat [04-events.md §10](04-events.md#10-mengganti-rabbitmq-dengan-kafka)) |

### 2.5 Cache produk

`product-service` memakai **Caffeine** (cache lokal per instance). Konsekuensinya:

- Dengan 3 replika, ada 3 cache berbeda. Eviction di satu instance tidak menyentuh yang lain
- Untuk katalog yang berubah jarang, ini **dapat diterima** — TTL 2 menit membatasi staleness

**Kapan harus pindah ke Redis (cache terdistribusi):**
- Data berubah sering (flash sale, stok real-time)
- TTL pendek tapi tetap butuh konsistensi antar-instance
- Ingin menghemat memori (satu cache untuk semua replika)

Cara pindah: tambahkan `spring-boot-starter-data-redis` + konfigurasi
`RedisCacheManager` dengan `GenericJackson2JsonRedisSerializer`. Perhatikan: **semua
objek yang di-cache harus bisa diserialisasi JSON dengan aman**, jadi jangan simpan
entity JPA di cache — simpan DTO (kita sudah melakukannya).

---

## 3. Bottleneck tersembunyi yang sering menggigit

| Jebakan | Mengapa berbahaya | Deteksi |
|---|---|---|
| **Timeout tanpa batas** | Satu service lambat menahan semua thread | Pantau `http.client.requests` duration; set `readTimeout` (kita: 5s) |
| **Retry tanpa backoff** | Service yang sedang sakit dihujani request | Exponential backoff + jitter |
| **N+1 query** | 12 produk = 13 query; 1000 produk = bencana | Aktifkan `show-sql`, hitung query |
| **transaksi panjang menahan lock** | Semua penulis lain menunggu | Pantau `pg_stat_activity` |
| **Log tanpa struktur** | Tidak bisa dicari | Pakai JSON log + correlation id |
| **Cache stampede** | Cache kedaluwarsa bersamaan → DB dihujani | TTL + jitter |
| **Retry ganda** (Feign + Resilience4j + client) | 3 lapis retry = 27 percobaan | Retry **hanya** di satu lapis |

---

## 4. Observability yang layak produksi

### Metrik (sudah ada)

Semua service mengekspos `/actuator/prometheus`. Yang perlu dipantau lebih dulu:

```promql
# Tingkat error per service
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)

# Latensi p95
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))

# Circuit breaker terbuka
resilience4j_circuitbreaker_state{state="open"} == 1

# Pesan menumpuk di queue (perlu plugin RabbitMQ Prometheus — sudah aktif)
rabbitmq_queue_messages_ready

# Pool koneksi database yang terpakai
hikaricp_connections_active / hikaricp_connections_max
```

> **Aturan alert:** alert pada **gejala yang dirasakan user** (error rate, latensi p95),
> bukan pada penyebab (CPU tinggi). CPU 100% tapi user senang = tidak perlu bangun jam 3 pagi.

### Tracing terdistribusi

Correlation id sudah ada, tapi belum ada *distributed tracing*. Untuk menambahkannya:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

Lalu tambahkan Tempo/Jaeger ke `docker-compose.yml`. Spring Boot otomatis meneruskan
trace context lewat **header HTTP, Feign, dan RabbitMQ** — kamu tidak perlu mengubah
business logic. Ini upgrade dengan rasio manfaat/biaya terbaik setelah Prometheus.

### Logging terpusat

Saat ini log tersebar di 10 container. Untuk produksi, pilih salah satu:

| Opsi | Kelebihan | Kekurangan |
|---|---|---|
| **Grafana Loki** | Ringan, sudah satu ekosistem dengan Grafana | Query bahasa sendiri (LogQL) |
| **ELK** | Sudah sangat matang, pencarian kuat | Berat (Elasticsearch butuh RAM besar) |
| **OpenSearch** | Fork ELK, lisensi lebih permisif | Sama seperti ELK |

**Minimal yang harus dilakukan:** keluarkan log sebagai JSON supaya bisa di-parse.

```yaml
logging:
  structured:
    format:
      console: ecs     # Spring Boot 3.4+ punya structured logging bawaan
```

---

## 5. Kubernetes

`infra/k8s/` berisi kerangka manifest. Yang perlu diperhatikan khusus untuk Java:

```yaml
resources:
  requests: { memory: "512Mi", cpu: "250m" }
  limits:   { memory: "1Gi" }        # JANGAN set CPU limit — bikin throttling

livenessProbe:                        # "apakah proses ini harus di-restart?"
  httpGet: { path: /actuator/health/liveness, port: 8080 }
  initialDelaySeconds: 40
  periodSeconds: 10

readinessProbe:                       # "apakah boleh menerima trafik?"
  httpGet: { path: /actuator/health/readiness, port: 8080 }
  initialDelaySeconds: 20
  periodSeconds: 5

# Graceful shutdown: beri waktu request yang sedang jalan untuk selesai
terminationGracePeriodSeconds: 45
```

Aktifkan agar Kubernetes bisa membaca probe:

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true     # sudah aktif di semua service
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

**Kenapa CPU limit berbahaya untuk JVM:** JVM memakai CPU limit untuk menghitung jumlah
thread GC dan compiler JIT. Dengan limit rendah, startup bisa 10× lebih lambat dan
GC jadi tidak efisien. Batasi memori (`MaxRAMPercentage` sudah diset di Dockerfile),
biarkan CPU request yang mengatur penjadwalan.

**Kenapa `terminationGracePeriodSeconds` penting:** `spring.lifecycle.timeout-per-shutdown-phase`
sudah 20s di semua service. Tanpa grace period di Kubernetes, pod dibunuh paksa di
tengah request dan user melihat error 502.

---

## 6. CI/CD

`.github/workflows/ci.yml` sudah menjalankan:

1. **Backend** — `./mvnw verify` (compile semua modul + unit test)
2. **Frontend** — `pnpm lint` + `pnpm build`
3. **Docker** — smoke build 2 image (bukan 9, supaya CI tidak lambat)

Yang perlu ditambahkan:

| Tahap | Alat | Kenapa |
|---|---|---|
| Pemindaian dependency | Dependabot / OWASP Dependency-Check | Kerentanan CVE |
| Pemindaian image | Trivy | Base image bisa punya CVE |
| Analisis kualitas | SonarQube Cloud | Bug & code smell |
| Push image | GHCR / ECR, tag dengan commit SHA | Traceability |
| Deployment | ArgoCD (GitOps) | Rollback = `git revert` |
| Migrasi database | Flyway di pipeline, **sebelum** deploy | Hindari skema & kode tidak sinkron |

**Aturan penting:** migrasi database harus **backward compatible** dengan versi kode
sebelumnya (`expand → migrate → contract`), karena saat rolling deploy kedua versi
akan berjalan bersamaan.

---

## 7. Urutan optimasi yang benar

Jangan optimasi berdasarkan dugaan. Urutannya:

```
1. UKUR          → pasang metrik, lihat p95 dan error rate
2. TEMUKAN       → service & query mana yang lambat
3. PERBAIKI      → indeks, cache, hilangkan N+1, pendekkan transaksi
4. UKUR LAGI     → apakah benar membaik?
5. SKALA         → baru tambah replika / shard
```

**90% masalah performa di sistem seperti ini adalah satu dari tiga hal:**
1. Query tanpa indeks
2. N+1 query
3. Memanggil service lain di dalam transaksi database

Ketiganya sudah diberi contoh penanganannya di repo ini, lengkap dengan komentar
"kenapa".

---

## 8. Checklist sebelum produksi

Gunakan sebagai daftar periksa. Yang dicentang sudah ada di base ini.

**Keamanan**
- [x] JWT di httpOnly cookie (bukan localStorage)
- [x] Gateway menghapus header identitas palsu dari klien
- [x] Endpoint internal dilindungi token bersama
- [x] Port service tidak dipublikasikan ke luar
- [x] Error 500 tidak membocorkan detail internal
- [ ] Rate limiting di endpoint auth
- [ ] Refresh token & rotasi
- [ ] RS256 + JWKS (bukan HS256)
- [ ] mTLS antar-service atau verifikasi JWT di tiap service
- [ ] Secret dari Vault / AWS Secrets Manager (bukan `.env`)
- [ ] Audit log untuk aksi admin
- [ ] HTTPS/TLS termination di ingress

**Operasional**
- [x] Health check per service
- [x] Correlation id lintas service
- [x] Metrik Prometheus
- [x] Dead-letter queue
- [x] Graceful shutdown (`server.shutdown: graceful`)
- [x] Dockerfile menjalankan proses sebagai non-root
- [x] `MaxRAMPercentage` untuk JVM di container
- [ ] Tracing terdistribusi (OpenTelemetry)
- [ ] Logging terpusat (Loki/ELK)
- [ ] Alert berbasis SLO
- [ ] Backup database terjadwal + uji restore
- [ ] Runbook untuk insiden umum

**Data**
- [x] Database per service
- [x] Flyway (migrasi berversi, bisa di-review)
- [x] `ddl-auto: validate`
- [x] Unique constraint untuk idempoten
- [x] `CHECK` constraint untuk integritas (stok ≥ 0)
- [ ] Transactional Outbox (lihat [04-events.md](04-events.md#outbox))
- [ ] Job rekonsiliasi untuk stok yang "tertahan"
- [ ] Strategi retensi data (pesanan lama, log, metrik)

**Proses**
- [x] CI menjalankan build + test
- [x] `.dockerignore` & build cache
- [x] Satu Dockerfile untuk semua service (tidak ada duplikasi)
- [ ] Branch protection + wajib review
- [ ] Conventional commits & changelog otomatis
- [ ] ArgoCD / GitOps dengan rollback satu perintah
