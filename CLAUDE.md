# CLAUDE.md

Project context and working rules for Claude Code in `auction_mvp`.

## Repository layout (monorepo)

```
auction_mvp/
├── backend/                  # Java 25, Spring Boot 3.5, Maven multi-module
│   ├── auction-domain/       # pure Java domain model + ports
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

### Persistence rules
- **Soft delete only — never hard delete.** Mark records as deleted (e.g. `deletedAt` / `deleted` flag) and exclude them from queries by default. No `DELETE` statements, no `repository.delete(...)`.

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

Base package: `com.auction` (`com.auction.domain`, `com.auction.infrastructure`, `com.auction.app`).

```bash
cd backend
mvn verify                                   # full build + tests (definition of done)
java -jar auction-app/target/auction-app-0.0.1-SNAPSHOT.jar   # run the app after verify (needs PostgreSQL; DB_URL/DB_USERNAME/DB_PASSWORD, defaults localhost:5432/auction, auction/auction)
```

- Library versions live in `backend/pom.xml` only; child POMs declare dependencies without versions.
- `auction-domain` has a `maven-enforcer` rule that fails the build on any Spring / JPA / Hibernate dependency.
- The context test uses H2 through the `test` profile (`auction-app/src/test/resources/application-test.yml`).
- Architecture rules are enforced by `auction-app/src/test/java/com/auction/app/architecture/ArchitectureTest.java` (ArchUnit): domain depends only on the JDK and itself, infrastructure never depends on app, controllers never touch repositories (Spring Data, `@Repository`, or `*Repository` ports) and go through use cases instead.
- To build or test a single module, add `-am` so sibling modules are built too, e.g. `mvn -pl auction-app -am test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`.
