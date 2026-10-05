# =====================================================================
# ShopFlow - perintah yang sering dipakai
#   make            -> tampilkan daftar perintah
#   make up         -> jalankan seluruh infrastruktur + backend
#   make dev-user   -> jalankan SATU service di host (hot reload)
# =====================================================================

SHELL := /bin/bash
BACKEND := backend
FRONTEND := frontend/web
MVN := ./mvnw
COMPOSE := docker compose

# Token & secret default untuk development (sama dengan docker-compose.yml)
export JWT_SECRET ?= shopflow-development-secret-key-change-me-32chars
export INTERNAL_API_TOKEN ?= shopflow-internal-dev-token

.DEFAULT_GOAL := help
.PHONY: help up down restart logs ps build test verify lint fmt \
        infra infra-down reset-data eureka rabbit prometheus grafana \
        fe-install fe-dev fe-build dev-% native

# ---------------------------------------------------------------------
help: ## Tampilkan semua perintah yang tersedia
	@grep -hE '^[a-zA-Z_%-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| sort \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

# ---------------------------------------------------------------------
# Docker
# ---------------------------------------------------------------------
up: ## Build & jalankan infrastruktur + semua service backend
	$(COMPOSE) up -d --build

up-frontend: ## Ikut menjalankan Next.js di dalam Docker (build lambat)
	$(COMPOSE) --profile frontend up -d --build

down: ## Hentikan semua container (data tetap tersimpan)
	$(COMPOSE) down

down-frontend: ## Hentikan termasuk profil frontend
	$(COMPOSE) --profile frontend down

restart: ## Restart semua container
	$(COMPOSE) restart

logs: ## Ikuti log semua service
	$(COMPOSE) logs -f --tail=100

ps: ## Status container
	$(COMPOSE) ps

infra: ## Jalankan HANYA postgres, redis, rabbitmq (untuk development di host)
	$(COMPOSE) up -d postgres redis rabbitmq

infra-down: ## Hentikan hanya infrastruktur
	$(COMPOSE) stop postgres redis rabbitmq

reset-data: ## HAPUS semua volume (database, redis, rabbitmq) - hati-hati!
	$(COMPOSE) down -v
	@echo "Semua volume dihapus. Jalankan 'make up' untuk memulai dari nol."

# ---------------------------------------------------------------------
# Backend
# ---------------------------------------------------------------------
build: ## Build semua modul backend (tanpa test)
	cd $(BACKEND) && $(MVN) -B -DskipTests package

test: ## Jalankan unit test backend
	cd $(BACKEND) && $(MVN) -B test

verify: ## Build + test + verifikasi lengkap
	cd $(BACKEND) && $(MVN) -B verify

native: ## Jalankan test ber-tag "integration" (butuh Docker/Testcontainers)
	cd $(BACKEND) && $(MVN) -B -Pit verify

dev-%: ## Jalankan satu service di host, mis. `make dev-product-service`
	cd $(BACKEND) && $(MVN) -B -pl $* spring-boot:run

# ---------------------------------------------------------------------
# Frontend
# ---------------------------------------------------------------------
fe-install: ## Install dependency frontend
	cd $(FRONTEND) && pnpm install

fe-dev: ## Jalankan Next.js dev server di http://localhost:3000
	cd $(FRONTEND) && pnpm dev

fe-build: ## Build produksi frontend
	cd $(FRONTEND) && pnpm build

lint: ## Lint frontend
	cd $(FRONTEND) && pnpm lint

# ---------------------------------------------------------------------
# Tautan cepat
# ---------------------------------------------------------------------
eureka: ## Buka Eureka dashboard
	@echo "http://localhost:8761"

rabbit: ## Buka RabbitMQ Management UI
	@echo "http://localhost:15672  (shopflow / shopflow)"

prometheus: ## Buka Prometheus
	@echo "http://localhost:9090"

grafana: ## Buka Grafana
	@echo "http://localhost:3001  (admin / admin)"

gateway: ## Cek kesehatan API Gateway
	@curl -s http://localhost:8080/actuator/health || echo "Gateway belum jalan"
