# 🏗️ Production-Grade Spring Boot Microservices Platform

## Architecture
```
Client → API Gateway (Port 8080)
           ↓
    Eureka Service Discovery (Port 8761)
           ↓
    ┌─────────────────────────────────────┐
    │  auth-service    (8081)             │
    │  user-service    (8084)             │
    │  product-service (8082)             │
    │  order-service   (8083)             │
    │  notification-service (8085)        │
    └─────────────────────────────────────┘
    Config Server (8888) | Redis | PostgreSQL | Kafka
```

## Features
| Feature | Implementation |
|---|---|
| **JWT Security** | jjwt, BCrypt-12, token blacklist in Redis, refresh tokens |
| **Service Discovery** | Netflix Eureka |
| **Load Balancing** | Spring Cloud LoadBalancer (lb://) |
| **API Gateway** | Spring Cloud Gateway |
| **Rate Limiting** | Redis token bucket (per-user + per-IP) |
| **Circuit Breaker** | Resilience4j COUNT_BASED |
| **Retry** | Resilience4j exponential backoff |
| **Bulkhead** | Resilience4j semaphore-based |
| **Caching** | Spring Cache + Redis with TTL |
| **Async Messaging** | Kafka with manual ACK, idempotent producer |
| **Async Threads** | @Async + ThreadPoolTaskExecutor |
| **Database** | PostgreSQL + HikariCP + Flyway migrations |
| **Observability** | Actuator + Prometheus + Grafana |

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven 3.8+

### Run with Docker Compose
```bash
# 1. Copy environment file
cp .env.example .env
# Edit .env with your secrets

# 2. Build all services
mvn clean package -DskipTests

# 3. Start full stack
docker-compose up -d

# 4. Verify
curl http://localhost:8761          # Eureka Dashboard
curl http://localhost:8080/actuator/health  # API Gateway
```

### API Usage
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"john","email":"john@example.com","password":"SecureP@ss1"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"john","password":"SecureP@ss1"}'

# Use token
curl http://localhost:8080/api/products \
  -H 'Authorization: Bearer <your-jwt-token>'

# Create order
curl -X POST http://localhost:8080/api/orders \
  -H 'Authorization: Bearer <your-jwt-token>' \
  -H 'Content-Type: application/json' \
  -d '{"items":[{"productId":1,"quantity":2}],"shippingAddress":"123 Main St"}'
```

## Monitoring
| Dashboard | URL |
|---|---|
| Eureka | http://localhost:8761 |
| Kafka UI | http://localhost:9090 |
| Prometheus | http://localhost:9091 |
| Grafana | http://localhost:3000 (admin/admin) |

## Service Ports
| Service | Port |
|---|---|
| API Gateway | 8080 |
| Auth Service | 8081 |
| Product Service | 8082 |
| Order Service | 8083 |
| User Service | 8084 |
| Notification Service | 8085 |
| Config Server | 8888 |
| Eureka Server | 8761 |
| PostgreSQL (auth) | 5432 |
| PostgreSQL (orders) | 5433 |
| PostgreSQL (products) | 5434 |
| Redis | 6379 |
| Kafka | 9092 |
| Kafka UI | 9090 |
| Prometheus | 9091 |
| Grafana | 3000 |
