# CLAUDE.md

Project context and working rules for Claude Code in `auction_mvp`.

## Repository layout (monorepo)

```
auction_mvp/
├── backend/                  # Java 25, Spring Boot 3.5, Maven multi-module
│   ├── auction-domain/       # pure Java: domain model + ports, and application use cases
│   ├── auction-infrastructure/  # adapters implementing the domain ports
│   └── auction-app/          # Spring Boot app: controllers + configuration
├── frontend/                 # Vue 3 + TypeScript + Vite
├── docker/keycloak/          # Keycloak realm export (imported on startup)
├── .github/workflows/ci.yml  # CI on every push: backend verify, frontend test + build, Docker images
└── docker-compose.yml        # full stack: PostgreSQL (5433), Keycloak (8180), Kafka (9094), backend (8080), frontend (5173)
```

## Architecture

### Hexagonal + DDD
- **auction-domain**: pure Java only. **Zero Spring, JPA, or other framework dependencies** — no Spring annotations, no `jakarta.persistence`, no Lombok-generated framework coupling. Contains aggregates, entities, value objects, domain services, domain events, and the **ports** (interfaces) the domain needs.
- **auction-infrastructure**: **adapters** that implement the domain ports (persistence, messaging, external services). JPA entities live here and are mapped to/from domain objects; they never leak into the domain.
- **auction-app**: REST controllers and Spring configuration (wiring use cases to adapters). No business logic here.
- Dependency direction: `app → infrastructure → domain`, and `app → domain`. The domain depends on nothing.

### CQRS (light)
- Commands and queries are **separate use cases**: a command use case changes state and returns nothing (or just an id); a query use case reads state and never mutates it.
- Keep command and query ports/classes in separate packages; don't mix reads and writes in one use case.
- Use cases live in `auction-domain` under `com.auction.application.command` / `com.auction.application.query` (plain classes, no Spring annotations; `auction-app` wires them as beans). Each takes a command/query record and has a single `handle(...)` method.
- Write ports (`AuctionRepository`, `BidRepository`, `UserRepository`) and the read port (`AuctionReadRepository` → `AuctionSummary`) live in the domain packages. Paging uses the domain `PageQuery` / `Page`, not Spring's.
- Use cases get time from an injected `Clock`.

### Persistence rules
- **Soft delete only — never hard delete.** Every table has a `deleted` flag. Entities carry `@SQLRestriction("deleted = false")` (flagged rows are invisible to all JPA reads) and `@SQLDelete` (a JPA remove becomes an UPDATE). Spring Data interfaces extend the bare `Repository` and declare only the methods they need, so no delete method exists. No `DELETE` statements in production code.
- Schema is owned by **Flyway** (`auction-infrastructure/src/main/resources/db/migration`); Hibernate runs with `ddl-auto: validate`. Never edit an applied migration; add a new `V<n>__*.sql`. Keep SQL valid on both PostgreSQL and H2 (PostgreSQL mode).
- JPA entities (`*Entity`) live in `com.auction.infrastructure.persistence.*` and are mapped by static mappers (`*Mapper`); domain objects are rebuilt with `restore(...)` factories.
- **Optimistic locking on `auctions`** (`@Version`): `AuctionPersistenceAdapter.save` rejects a domain object loaded at an older version and translates Hibernate conflicts into the domain `AuctionConcurrentlyModifiedException`. A use case that saves several aggregates (e.g. `PlaceBid`: auction then bid) must run in one transaction.

### Domain events, Kafka, live updates
- Aggregates record events (`BidPlaced`, `AuctionClosed`); use cases call `aggregate.pullEvents()` **after saving** and hand them to the `DomainEventPublisher` port. The domain stays pure Java.
- `SpringDomainEventPublisher` (infra) turns them into Spring application events; `KafkaDomainEventRelay` sends them with `@TransactionalEventListener(AFTER_COMMIT)` to `auction.bids` / `auction.closed` (key = auction id, JSON wire records `*Message`). Never publish to Kafka inside the transaction. Delivery is at-most-once (no outbox yet).
- `KafkaAuctionEventListener` (unique consumer group per instance, latest offset) calls the `AuctionEventSubscriber` inbound port; `AuctionStreamBroadcaster` (app) implements it and pushes SSE to `GET /api/auctions/{id}/stream` (`connected` / `bid` / `closed`, heartbeat every 20 s). Running auctions carry a `stream` HAL link.
- Frontend: SSE is read with `fetch` (bearer token; `EventSource` cannot send headers) — `api/sse.ts`, `useAuctionStream`.

### REST API (`auction-app`)
- Controllers in `com.auction.app.web.*` call use cases only (never repositories) and map request DTOs (records with Bean Validation) to commands/queries. Format checks live in DTOs; business rules stay in the domain.
- Use cases are registered in `UseCaseConfig` and wrapped by `TransactionalProxies` (commands read-write, queries read-only). Add every new use case there.
- Responses are HAL (`spring-boot-starter-hateoas`): `EntityModel` with `self` / `bids` / `place-bid` (only while open for bidding) links; paged lists via `PagedModels` (self/prev/next + `page` metadata). An empty page has no `_embedded` key.
- Errors are RFC 9457 `ProblemDetail` from `ApiExceptionHandler`, `type` = `urn:bidanddrive:problem:<slug>`: 400 validation / invalid-value, 403 forbidden (role rules), 404 not-found, 409 bid-too-low / auction-not-open / concurrent-modification, 422 for any unmapped domain rule. Map each new domain exception explicitly.
- **Security:** OAuth2 resource server with Keycloak JWTs (`web.security.SecurityConfig`). `GET /api/auctions/running` and the API docs are public; everything else needs a bearer token. Realm roles (`realm_access.roles`) become `ROLE_SELLER` / `ROLE_BUYER` (`KeycloakRealmRoleConverter`); `POST /api/auctions` needs SELLER, `POST .../bids` needs BUYER. Issuer and audience (`auction-api`) are validated.
- The caller's id is the token `sub`. Controllers take `@CurrentUser UserId`, which provisions or updates the local user from the token claims (`name`, `email`, `phone_number`, exactly one auction role) via `ProvisionUserUseCase`; missing claims give 403 `incomplete-identity`. Never take the user id from the request.
- Spring Security's 401/403 are written as `ProblemDetail` too (`SecurityProblemHandler`), keeping the `WWW-Authenticate` header.
- OpenAPI via springdoc at `/v3/api-docs` and `/swagger-ui/index.html`; declare status codes with `@ApiResponse`; error bodies are documented as `ProblemDetail` automatically.

### Frontend (`frontend/`)
- Vue 3 + TypeScript + Vite, Pinia, vue-router, keycloak-js. Versions are pinned to what runs on **Node 20.18** (Vite 6, Vitest 3, vue-router 4, Pinia 3); newer majors need Node ≥ 20.19 / 22.
- `src/api/`: `apiFetch` (adds the bearer token, throws `ApiError` carrying the backend `ProblemDetail`), HAL helpers (`linkTo`, `requireLink`, `embedded`), types. **Follow HAL links**; only entry points live in `api/endpoints.ts` (`/api/auctions/running`, `/api/auctions`, and `auctionUrl(id)` for deep links).
- Composables (`src/composables/`) do API calls with their own `loading`/`error` state. Pinia only for shared state: `auth` (user, realm roles, token refresh) and `auction` (current auction + bids).
- Show errors with `ProblemAlert`; form field errors come from `ApiError.fieldErrors`. Offer actions only when the server sends the link (e.g. bidding needs `place-bid`).
- In dev, Vite proxies `/api` to the backend (`API_PROXY_TARGET`, default `http://localhost:8080`) and keeps the Host header, so HAL links point at `localhost:5173`; no CORS needed. Keycloak settings: `VITE_KEYCLOAK_URL` / `VITE_KEYCLOAK_REALM` / `VITE_KEYCLOAK_CLIENT_ID`.
- Tests: Vitest (node environment) in `src/**/__tests__/*.spec.ts`, `fetch` stubbed with `vi.stubGlobal`, keycloak-js mocked with `vi.mock('@/auth/keycloak')`, HAL/problem fixtures in `src/testing/fixtures.ts`.

## Workflow

### TDD
1. Write the failing test first.
2. Write the minimum code to make it pass.
3. Refactor with tests green.

Domain logic gets plain unit tests (no Spring context).

### Commits
- **Conventional Commits** (`feat:`, `fix:`, `test:`, `refactor:`, `chore:`, `docs:`, `build:` …), optionally scoped, e.g. `feat(domain): place bid on open auction`.
- **One small commit per step.**
- **Never commit if the build is red.**

### Definition of done
- **Always run `mvn verify` (from `backend/`) and confirm it passes before saying a task is done.** Report failures with their output instead of claiming success.
- For frontend changes, also run `npm test` and `npm run build` (type-check + bundle) in `frontend/`.

## Commands

Base package: `com.auction` (`com.auction.domain`, `com.auction.application`, `com.auction.infrastructure`, `com.auction.app`).

```bash
./run.sh                                     # build + start the full stack and wait until it answers (./run.sh --help)
docker compose up -d --build                 # full stack: app on http://localhost:5173, API on 8080, Keycloak on 8180 (admin/admin)
docker compose up -d postgres keycloak kafka # infrastructure only, to run the backend / Vite locally
cd backend
mvn verify                                   # full build + tests (definition of done)
java -jar auction-app/target/auction-app-0.0.1-SNAPSHOT.jar   # run the app after verify; defaults match docker compose
# token for a test user (seller/seller, buyer1/buyer1, buyer2/buyer2, buyer3/buyer3; see README):
curl -s -X POST http://localhost:8180/realms/bidanddrive/protocol/openid-connect/token \
  -d grant_type=password -d client_id=auction-frontend -d username=seller -d password=seller
```

```bash
cd frontend
npm install
npm run dev                                  # http://localhost:5173 (backend on 8080 + postgres/keycloak running; stop the compose frontend first, same port)
npm test                                     # Vitest
npm run build                                # vue-tsc type-check + production bundle
```

- App settings (env): `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` (default `localhost:5433/auction`, `auction`/`auction`), `OAUTH2_ISSUER_URI` (default `http://localhost:8180/realms/bidanddrive`), `OAUTH2_AUDIENCE` (default `auction-api`). Compose ports: `POSTGRES_PORT`, `KEYCLOAK_PORT`.
- Docker: `backend/Dockerfile` (Maven build, tests skipped — CI runs them; Java 25 JRE runtime) and `frontend/Dockerfile` (Vite build, nginx). nginx proxies `/api` with the browser's Host so HAL links stay on `localhost:5173`. In compose the backend validates the issuer `http://localhost:8180/...` (what the browser sees) but fetches JWKS from `http://keycloak:8080` (`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI`). Keycloak settings are compiled into the frontend bundle (build args `VITE_KEYCLOAK_*`).
- `run.sh` wraps docker compose (prerequisites, port check, readiness wait, URLs); keep its ports and URLs in sync with `docker-compose.yml`.
- CI (`.github/workflows/ci.yml`) runs the same commands as the definition of done; keep them in sync when changing build steps.
- The realm export is the source of truth for roles, clients and test users: edit `docker/keycloak/bidanddrive-realm.json`, then `docker compose down && docker compose up -d` (it is only imported when the realm does not exist).

- Library versions live in `backend/pom.xml` only; child POMs declare dependencies without versions.
- `auction-domain` has a `maven-enforcer` rule that fails the build on any Spring / JPA / Hibernate dependency.
- The context test uses H2 through the `test` profile (`auction-app/src/test/resources/application-test.yml`); Flyway migrates it and Hibernate validates the mapping.
- API tests extend `ApiIntegrationTest` (full app, MockMvc, H2, tables cleared before each test; seed users through `UserRepository`). Authenticate with `tokenOf(user)` / `token(sub, claims, roles...)` / `anyToken()`: `jwt()` mocks with Keycloak-shaped claims, mapped by the real `KeycloakRealmRoleConverter`.
- Infrastructure tests run against real PostgreSQL and Kafka via Testcontainers (Docker required): extend `InfrastructureIntegrationTest` (one shared `postgres:17-alpine` and `apache/kafka:3.9.2` container, tables truncated before each test). Testcontainers 1.21 cannot start Kafka 4.x brokers; keep the broker on the kafka-clients version.
- The H2 app tests run with `auction.kafka.enabled=false` (no broker): the relay, consumer and topics are off, so SSE tests hand events to `AuctionStreamBroadcaster` directly.
- Architecture rules are enforced by `auction-app/src/test/java/com/auction/app/architecture/ArchitectureTest.java` (ArchUnit): domain depends only on the JDK and itself, application only on domain + JDK, command and query use cases don't depend on each other, infrastructure never depends on app, controllers never touch repositories (Spring Data, `@Repository`, or `*Repository` ports) and go through use cases instead.
- To build or test a single module, add `-am` so sibling modules are built too, e.g. `mvn -pl auction-app -am test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`.
