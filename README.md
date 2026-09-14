# E-commerce Microservices

## Local startup

1. Start infrastructure with `docker compose up -d`.
2. Start `config-server`, then `discovery`, then `gateway`.
3. Start the business services.

Docker PostgreSQL is exposed on port `5433` to avoid conflicts with a local PostgreSQL on the default port `5432`. On a new Docker volume, `docker/postgres/init-databases.sql` creates the `product`, `payment`, and `order` databases.

If the PostgreSQL volume already existed before the initialization script was added, recreate only when its data is disposable with `docker compose down -v && docker compose up -d`, or create the three databases manually.

Use plural API paths consistently:

- `/api/v1/products`
- `/api/v1/payments`
- `/api/v1/orders`

Before debugging service code, verify dependencies with `docker compose ps`, check Eureka at `http://localhost:8761`, and confirm the service received its remote configuration from `http://localhost:8888/<service-name>/default`.
