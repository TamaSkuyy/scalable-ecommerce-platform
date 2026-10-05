# Kubernetes

Kerangka manifest untuk men-deploy ShopFlow ke Kubernetes. Ini **titik awal yang sengaja
dibuat sederhana** — cukup untuk belajar, belum untuk produksi.

> Sebelum memakai folder ini, baca
> [docs/06-scalability-and-production.md §5](../docs/06-scalability-and-production.md).
> Ada tiga jebakan khusus Java di Kubernetes: **CPU limit**, **probe**, dan
> **graceful shutdown**. Semuanya sudah ditangani di manifest di sini.

---

## Isi

| File | Isi |
|---|---|
| `namespace.yaml` | Namespace `shopflow` |
| `configmap.yaml` | Konfigurasi non-rahasia (URL Eureka, host RabbitMQ, dll.) |
| `discovery-service.yaml` | Deployment + Service Eureka (semua service menunjuk ke sini) |
| `api-gateway.yaml` | Deployment + Service Gateway (satu-satunya yang terbuka ke luar) |
| `product-service.yaml` | Deployment + Service + HPA + PDB (contoh lengkap satu service) |
| `ingress.yaml` | Ingress + TLS (butuh ingress controller & cert-manager) |

**Secret tidak ada di repo ini** (sudah masuk `.gitignore`). Buat manual:

```bash
kubectl -n shopflow create secret generic shopflow-secrets \
  --from-literal=POSTGRES_PASSWORD='<kuat>' \
  --from-literal=RABBITMQ_PASSWORD='<kuat>' \
  --from-literal=JWT_SECRET="$(openssl rand -base64 48)" \
  --from-literal=INTERNAL_API_TOKEN="$(openssl rand -base64 48)"
```

Di produksi, gunakan **External Secrets Operator** atau **SealedSecrets** agar secret
juga terkelola lewat Git tanpa menyimpan nilai mentahnya.

---

## Prasyarat

- Cluster Kubernetes (minikube / kind / k3s cukup)
- Ingress controller (nginx) — untuk `ingress.yaml`
- Image sudah di-push ke registry yang bisa diakses cluster

## Build & push image

```bash
REGISTRY=ghcr.io/USERNAME

for MODULE in api-gateway user-service product-service cart-service \
              order-service payment-service notification-service discovery-service; do
  docker build -f backend/Dockerfile \
    --build-arg MODULE=$MODULE \
    -t $REGISTRY/shopflow-$MODULE:0.1.0 ./backend
  docker push $REGISTRY/shopflow-$MODULE:0.1.0
done
```

Lalu ganti `image:` di setiap manifest sesuai registry-mu.

## Deploy

```bash
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f api-gateway.yaml
kubectl apply -f product-service.yaml
kubectl apply -f ingress.yaml

kubectl -n shopflow get pods -w
kubectl -n shopflow logs -f deploy/api-gateway
```

## Yang belum ada di folder ini

Ini sengaja dikosongkan sebagai latihan (lihat
[docs/07-roadmap.md](../docs/07-roadmap.md) Tahap 5):

- Manifest untuk service lain (pola-nya identik dengan `product-service.yaml`)
- PostgreSQL / Redis / RabbitMQ sebagai StatefulSet atau (lebih baik) layanan terkelola
- `NetworkPolicy` — hanya Gateway yang boleh masuk ke service lain
- `PodDisruptionBudget` untuk menjamin replika minimum saat maintenance node
- `HorizontalPodAutoscaler` berdasarkan kedalaman queue (butuh KEDA)
- Kustomize overlay untuk `staging` dan `production`

## Tips debugging

```bash
# Kenapa pod restart terus?
kubectl -n shopflow describe pod -l app=product-service | tail -30

# Cek probe health dari dalam pod
kubectl -n shopflow exec deploy/api-gateway -- \
  wget -qO- http://localhost:8080/actuator/health/readiness

# Lihat event terakhir di namespace
kubectl -n shopflow get events --sort-by=.lastTimestamp | tail -20

# Shell ke dalam pod
kubectl -n shopflow exec -it deploy/api-gateway -- sh
```
