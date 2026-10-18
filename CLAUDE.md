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
└── docker-compose.yml        # local stack
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

## Commands

Base package: `com.auction` (`com.auction.domain`, `com.auction.application`, `com.auction.infrastructure`, `com.auction.app`).

```bash
cd backend
mvn verify                                   # full build + tests (definition of done)
java -jar auction-app/target/auction-app-0.0.1-SNAPSHOT.jar   # run the app after verify (needs PostgreSQL; DB_URL/DB_USERNAME/DB_PASSWORD, defaults localhost:5432/auction, auction/auction)
```

- Library versions live in `backend/pom.xml` only; child POMs declare dependencies without versions.
- `auction-domain` has a `maven-enforcer` rule that fails the build on any Spring / JPA / Hibernate dependency.
- The context test uses H2 through the `test` profile (`auction-app/src/test/resources/application-test.yml`); Flyway migrates it and Hibernate validates the mapping.
- Infrastructure tests run against real PostgreSQL via Testcontainers (Docker required): extend `PostgresIntegrationTest` (one shared `postgres:17-alpine` container, tables truncated before each test).
- Architecture rules are enforced by `auction-app/src/test/java/com/auction/app/architecture/ArchitectureTest.java` (ArchUnit): domain depends only on the JDK and itself, application only on domain + JDK, command and query use cases don't depend on each other, infrastructure never depends on app, controllers never touch repositories (Spring Data, `@Repository`, or `*Repository` ports) and go through use cases instead.
- To build or test a single module, add `-am` so sibling modules are built too, e.g. `mvn -pl auction-app -am test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`.
