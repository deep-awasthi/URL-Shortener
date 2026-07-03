# URL Shortener

Production-grade URL shortener with PostgreSQL, Redis, Kafka, Elasticsearch, Bloom Filter, JWT auth, rate limiting, Resilience4j, Micrometer + Prometheus + Grafana, OpenTelemetry tracing, Flyway migrations, and Kubernetes manifests.

## Architecture

```
Client ──▶ URL Shortener (Spring Boot + Virtual Threads)
              │
              ├──▶ PostgreSQL   (persistent storage + Flyway migrations)
              ├──▶ Redis        (URL cache + Bucket4j rate limiting)
              ├──▶ Kafka        (async click analytics via KRaft)
              ├──▶ Elasticsearch (full-text URL search)
              └──▶ OTel Collector ──▶ Grafana / Prometheus
```

## Prerequisites

| Tool | Version | Install |
| --- | --- | --- |
| Java | 21+ | [adoptium.net](https://adoptium.net) |
| Maven | 3.9+ | or use `./mvnw` wrapper |
| Docker | latest | [docker.com](https://docker.com) |
| Docker Compose | v2 | included with Docker Desktop |

## Step 1 — Configure Credentials

Open `docker-compose.yml` and edit the `url-shortener` service:

```yaml
url-shortener:
  environment:
    # JWT secret — generate with: openssl rand -base64 48
    JWT_SECRET: <your-random-string-at-least-32-chars>

    # Admin token — protects /admin/urls API
    ADMIN_TOKEN: <your-admin-token>

    # Rate limits
    RATE_LIMIT_API: "10"        # max requests/sec per IP
    RATE_LIMIT_REDIRECT: "60"   # max redirects/min per short code
```

### Credential reference

| Variable | Location | Purpose | Default |
| --- | --- | --- | --- |
| `JWT_SECRET` | `docker-compose.yml` → `url-shortener` | HMAC key for JWT signing | **Must change** |
| `ADMIN_TOKEN` | `docker-compose.yml` → `url-shortener` | Static token for admin API | `change-me-before-production` |
| `DB_USER` / `DB_PASSWORD` | `docker-compose.yml` → `postgres` + `url-shortener` | PostgreSQL credentials | `postgres` / `postgres` |
| `GF_SECURITY_ADMIN_PASSWORD` | `docker-compose.yml` → `grafana` | Grafana login | `admin` |

### Generate a secure JWT secret

```bash
openssl rand -base64 48
# Copy the output and paste as JWT_SECRET
```

### Default admin user

A default admin is seeded via Flyway migration V4:

| Field | Value |
| --- | --- |
| Username | `admin` |
| Password | `admin123` |

Change this in production. The BCrypt hash is in `src/main/resources/db/migration/V4__seed_default_admin.sql`.

## Step 2 — Start the Stack

```bash
docker compose up --build
```

Wait for all services to be healthy (~60 seconds):

```bash
docker compose ps
# All 8 services should show "healthy" or "Up"
```

### Service URLs

| Service | URL | Credentials |
| --- | --- | --- |
| **URL Shortener** | http://localhost:8080 | — |
| **Admin Dashboard** | http://localhost:8080/admin | JWT token in the token field |
| **Prometheus** | http://localhost:9090 | — |
| **Grafana** | http://localhost:3000 | `admin` / `admin` |
| **Elasticsearch** | http://localhost:9200 | — |
| **Kafka** | localhost:9092 | — |
| **PostgreSQL** | localhost:5432 | `postgres` / `postgres` |
| **Redis** | localhost:6379 | — |
| **OTel Collector** | localhost:4317 / 4318 | — |

## Step 3 — Test the API

All sample requests are also in [`requests.http`](requests.http) (works with IntelliJ HTTP Client or VS Code REST Client).

### 3a. Register a user

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"deepa","password":"mypassword123"}'
```

### 3b. Login and get a JWT token

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

Response:

```json
{"token": "eyJhbGciOiJIUzM4NCJ9..."}
```

### 3c. Create a short URL

```bash
curl -X POST http://localhost:8080/api/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://github.com"}'
```

Response:

```json
{
  "id": "FJK3TYJM000008VPMDQJW8YMRT",
  "originalUrl": "https://github.com",
  "shortLink": "http://localhost:8080/h9rgddsSsu",
  "creationDate": "2026-07-03T19:59:51.288088",
  "expirationDate": "2026-08-02T19:59:51.288088",
  "clickCount": 0
}
```

With custom code and expiration:

```bash
curl -X POST http://localhost:8080/api/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com","customCode":"demo","expirationDate":"2026-12-31"}'
```

### 3d. Redirect (returns 302)

```bash
curl -i http://localhost:8080/demo
# → HTTP/1.1 302 Found
# → Location: https://example.com/
```

Each redirect increments the click count and sends an analytics event to Kafka.

### 3e. List all URLs

```bash
curl http://localhost:8080/api/urls
```

### 3f. Search URLs

```bash
curl "http://localhost:8080/api/urls/search?q=github"
```

### 3g. Health and metrics

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

## Step 4 — Use the Admin Dashboard

1. Get a JWT token (see Step 3b)
2. Open http://localhost:8080/admin
3. Paste the JWT token in the token field
4. Click **Save Token** then **Refresh**

Shows all URLs with status (Active/Expired), click counts, creation and expiration dates.

### Admin API with JWT

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/admin/urls
```

## Step 5 — Observability

### Prometheus

1. Open http://localhost:9090
2. Status > Targets — the `url-shortener` job should be **UP**
3. Key metrics:
   - `url_created_total` — total URLs created
   - `url_redirects_total` — total redirects served
   - `url_create_seconds` — time to create a URL
   - `url_redirect_seconds` — time to redirect

### Grafana

1. Open http://localhost:3000
2. Login: `admin` / `admin`
3. Data Sources > Add > Prometheus
4. URL: `http://prometheus:9090`
5. Save & Test
6. Create dashboards using the metrics above

### OpenTelemetry Tracing

Traces export to the OTel Collector at `http://otel-collector:4318`. Edit `otel-collector-config.yml` to forward to Jaeger, Tempo, or any OTLP-compatible backend.

## Step 6 — Kubernetes Deployment

### Deploy with Kustomize

```bash
# 1. Edit secrets — never commit real passwords
vi k8s/base/secret.yaml

# 2. Apply
kubectl apply -k k8s/overlays/dev    # 1 replica, 128Mi
kubectl apply -k k8s/overlays/prod   # 3 replicas, HPA 3-20
```

### Port-forward locally

```bash
kubectl port-forward svc/url-shortener-svc 8080:80 -n url-shortener
kubectl port-forward svc/grafana-svc 3000:3000 -n url-shortener
kubectl port-forward svc/prometheus-svc 9090:9090 -n url-shortener
```

### What gets deployed

| Resource | Description |
| --- | --- |
| Namespace | `url-shortener` |
| ConfigMap | Environment variables |
| Secret | DB password, JWT secret, admin token |
| StatefulSet | PostgreSQL + Kafka (persistent volumes) |
| Deployment | Redis, Elasticsearch, URL Shortener, Prometheus, Grafana |
| Service | ClusterIP for each service |
| Ingress | Nginx ingress at `short.example.com` |
| HPA | Auto-scale URL Shortener 2-10 (dev) or 3-20 (prod) |

## Step 7 — Production Checklist

- [ ] Set `JWT_SECRET` to a cryptographically random string (32+ bytes)
- [ ] Set `ADMIN_TOKEN` to a strong secret
- [ ] Change the default admin password (`admin` / `admin123`)
- [ ] Mount PostgreSQL, Redis, and Kafka data as persistent volumes
- [ ] Put the app behind a TLS-terminating reverse proxy
- [ ] Back up PostgreSQL regularly
- [ ] Tune Resilience4j thresholds for your traffic
- [ ] Adjust `RATE_LIMIT_API` and `RATE_LIMIT_REDIRECT`
- [ ] Change Grafana admin password
- [ ] Restrict `management.endpoints.web.exposure.include` in production

## Tech Stack

| Feature | Technology |
| --- | --- |
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 4.1 |
| Database | PostgreSQL 16 + Flyway migrations |
| Cache | Redis 7 + Spring Cache |
| Messaging | Apache Kafka 3.7 (KRaft mode) |
| Search | Elasticsearch 7.17 |
| Auth | JWT (jjwt) + Spring Security |
| Rate Limiting | Bucket4j (in-memory) |
| Resilience | Resilience4j (circuit breaker + retry) |
| Metrics | Micrometer + Prometheus |
| Tracing | OpenTelemetry (OTLP) |
| Bloom Filter | Guava (in-memory, 0.1% FPR) |
| ID Generation | ULID (26-char, time-ordered) |
| Containerization | Docker Compose |
| Orchestration | Kubernetes + Kustomize |

## License

MIT
