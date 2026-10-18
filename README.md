# BidAndDrive — car auction MVP

Sellers put cars up for auction, buyers bid, the highest bid wins.

| Part | Stack |
|---|---|
| Backend | Java 25, Spring Boot 3.5, Maven multi-module (`auction-domain`, `auction-infrastructure`, `auction-app`), PostgreSQL 17 + Flyway |
| Security | Spring Security OAuth2 resource server, JWTs from Keycloak 26 (realm roles `SELLER` / `BUYER`) |
| Frontend | Vue 3 + TypeScript + Vite, Pinia, vue-router, keycloak-js |
| Tests | JUnit 5, Mockito, ArchUnit, MockMvc (H2), Testcontainers (PostgreSQL), Vitest |
| Delivery | Dockerfiles, docker compose, GitHub Actions on every push |

---

## How to run

### One command

Requires Docker only.

```bash
./run.sh            # build the images, start everything, wait until it answers, print the URLs
./run.sh --test     # same, after running the backend and frontend test suites (needs Java 25, Maven, Node)
./run.sh stop       # stop, keep the data          ./run.sh clean   # stop and delete the data
./run.sh logs       # follow the logs              ./run.sh status  # list the containers
```

`run.sh` checks the prerequisites and that the ports are free, then runs the Docker compose stack below.

### Everything with Docker compose

```bash
docker compose up -d --build
```

| What | URL | Login |
|---|---|---|
| App | http://localhost:5173 | `seller` / `seller`, `buyer` / `buyer` |
| API + Swagger UI | http://localhost:8080/swagger-ui/index.html | bearer token (see below) |
| Keycloak admin | http://localhost:8180 | `admin` / `admin` |
| PostgreSQL | `localhost:5433`, db `auction` | `auction` / `auction` |

PostgreSQL is published on **5433** because 5432 is often taken; override with `POSTGRES_PORT`. The frontend has to stay on **5173**, the only redirect URI the Keycloak client allows.

`docker compose down -v` stops everything and deletes the data.

### Locally (backend from the IDE / jar, frontend with Vite)

Requires Java 25, Maven 3.9, Node ≥ 20.18, Docker (for PostgreSQL, Keycloak and the Testcontainers tests).

```bash
docker compose up -d postgres keycloak        # infrastructure only

cd backend
mvn verify                                    # build + all tests
java -jar auction-app/target/auction-app-0.0.1-SNAPSHOT.jar   # API on :8080 (defaults match compose)

cd ../frontend
npm install
npm run dev                                   # http://localhost:5173, proxies /api to :8080
```

Get a token to call the API directly (Swagger UI → *Authorize*, or curl):

```bash
curl -s -X POST http://localhost:8180/realms/bidanddrive/protocol/openid-connect/token \
  -d grant_type=password -d client_id=auction-frontend -d username=seller -d password=seller
```

Backend settings (environment variables): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OAUTH2_ISSUER_URI`, `OAUTH2_AUDIENCE`. Frontend build settings: `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID`.

### Tests

```bash
cd backend && mvn verify                      # 201 tests: domain/use cases, ArchUnit, MockMvc, Testcontainers
cd frontend && npm test && npm run build      # 34 Vitest tests, vue-tsc type-check, bundle
```

CI (`.github/workflows/ci.yml`) runs the same commands on every push, then builds both Docker images.

---

## Architecture

```
            ┌──────────────── auction-app ─────────────────┐
 HTTP ───▶  │ controllers, DTOs, HAL links, ProblemDetail,  │
            │ security, wiring of use cases + transactions  │
            └──────────────┬────────────────────────────────┘
                           │ calls
            ┌──────────── auction-domain (pure Java) ───────┐
            │ application: command / query use cases        │
            │ domain: User, Auction, Bid, value objects,    │
            │         rules, ports (repository interfaces)  │
            └──────────────▲────────────────────────────────┘
                           │ implements ports
            ┌──────── auction-infrastructure ───────────────┐
            │ JPA entities + mappers, Spring Data, adapters,│
            │ Flyway migrations                             │
            └───────────────────────────────────────────────┘
```

### Hexagonal + DDD

- **The domain is plain Java.** No Spring, JPA or Hibernate. A Maven Enforcer rule fails the build on such a dependency, and ArchUnit checks the imports.
- **Business rules live in the aggregates.**
  - `Auction` enforces "only a SELLER creates", "only a BUYER bids, never on their own auction", "bid > max(start price, highest bid)", "no bid outside [startDate, endDate) or when not RUNNING" and "close → ENDED + winner".
  - Value objects (`Money`, `Email`, `Phone`, `CarDetails`) reject invalid data at construction.
  - There are no setters, and every violation is a typed domain exception.
- **Ports and adapters.** The domain declares the repository ports. The infrastructure module implements them with JPA entities kept *separate* from the domain objects, connected by mappers.
  - Why: the domain model can evolve without database concerns, and the rules are tested in milliseconds without Spring.
- **Time is injected** (`Clock`), so time-based rules such as the bidding window are tested deterministically.
- **Boundaries are machine-checked.** ArchUnit (`ArchitectureTest`) enforces that:
  - the domain depends only on the JDK;
  - the use cases depend only on the domain;
  - infrastructure never depends on the app;
  - controllers never use repositories.

### CQRS (light)

- **Commands** change state and return at most an id: `CreateAuction`, `PlaceBid`, `CloseAuction`, `ProvisionUser`.
- **Queries** read and never write: `ListRunningAuctions`, `GetAuction`, `ListAuctionBids`.
- They use separate ports. Commands load the `Auction` aggregate through `AuctionRepository`; queries read `AuctionSummary` projections through `AuctionReadRepository`.
- Commands run in read-write transactions and queries in read-only ones. The domain module has no `@Transactional`; the app module wraps each use case in a transactional proxy.
- It's "light": one database and the same tables, with no event store or separate read database. That's enough to keep reads and writes from leaking into each other at MVP scale.

### Optimistic locking — why

Several buyers bid on the same car at the same moment, and each bid reads the auction (current highest bid) and then writes it.
- **Without protection:** two bids read version *n*, both pass the "higher than the current bid" check, and the last write silently overwrites the first. That's a lost update, and the highest bid can go *down*.
- **Why optimistic, not pessimistic:** conflicts are rare and short-lived, so optimistic locking (`@Version` on `auctions`) costs nothing on the normal path, holds no database locks during the request, and turns a race into a clear error.
- **How the adapter handles it:**
  - It refuses to save an auction that was loaded at an older version.
  - Hibernate's `UPDATE … WHERE version = ?` catches a commit that lands between that check and the flush.
  - Both cases become `AuctionConcurrentlyModifiedException`, and the API returns **409 Conflict**, so the client reloads and retries.
- **`ConcurrentBiddingTest` proves it** on real PostgreSQL: two threads, each in its own transaction, are held at a barrier until both have read version 0. Exactly one bid wins and only that bid is stored.
- **It also found a real bug.** The first adapter version re-read the row before saving, so a stale copy overwrote a newer bid (1500 → 1300) without any error. The version check above fixes it.

### Other choices

- **Persistence:** soft delete only. Every table has a `deleted` flag, `@SQLRestriction` hides flagged rows, `@SQLDelete` turns a JPA remove into an UPDATE, and the Spring Data interfaces expose no delete method. Flyway owns the schema and Hibernate only validates it (`ddl-auto=validate`), in tests too.
- **REST:**
  - Responses are HAL with links: `self`, `bids`, and `place-bid` only while bidding is open.
  - Errors are RFC 9457 `ProblemDetail` with a stable `type` (`urn:bidanddrive:problem:bid-too-low`, …).
  - The API is documented with springdoc OpenAPI.
- **Security:**
  - Keycloak realm roles are mapped to Spring roles.
  - The token `sub` is the user id. The user is created or updated from the token claims on their first write request.
  - Only `GET /api/auctions/running` (and the API docs) are public.
- **Frontend:**
  - Composables make the API calls; Pinia holds only shared state (the session and the current auction).
  - The UI follows the HAL links instead of building URLs; only entry points and deep links are hardcoded.
  - Problem details are shown as-is, with field errors next to their inputs.
  - `/api` is proxied (Vite in dev, nginx in Docker), so the browser stays on one origin and needs no CORS.

---

## User stories and acceptance criteria

Each criterion is covered by an automated test; the main test class is given.

### US1 — As a visitor, I browse the running auctions
- The list is public; no login is needed.
- It shows only auctions that are RUNNING and currently open (start ≤ now < end); not-started, past-end, cancelled, ended and deleted auctions are excluded.
- They're ordered by the soonest end date and paged (default size 20, max 100); `next`/`prev` links let you navigate.
- Invalid paging (`size=500`, `page=-1`) → 400 problem.
- *Tests:* `QueryAuctionsApiTest`, `AuctionReadPersistenceAdapterTest`, `useRunningAuctions.spec.ts`

### US2 — As a seller, I put a car up for auction
- Only a user with the SELLER role can create an auction (buyer → 403).
- The car name, condition, years, start price and dates are required; the description is optional.
- The rules are checked:
  - the start price is > 0 with at most 2 decimals;
  - the end date is after the start date;
  - the registration year is ≥ the year made.
  - Violations → 400 with the offending fields.
- The new auction is RUNNING with no bid. The response is 201 with `Location` and links; `place-bid` is present only if the auction has already started.
- *Tests:* `AuctionCreationTest`, `CarDetailsTest`, `CreateAuctionUseCaseTest`, `CreateAuctionApiTest`, `SecurityApiTest`, `useCreateAuction.spec.ts`

### US3 — As a buyer, I bid on a car
- Only a BUYER can bid, and a seller can never bid on their own auction → 403.
- A bid must be strictly greater than both the start price and the current highest bid → otherwise 409 `bid-too-low`.
- No bid before the start, at or after the end, or on an ENDED/CANCELLED auction → 409 `auction-not-open`.
- An accepted bid becomes the highest bid and appears in the bid history (newest first).
- The UI only shows the bid form when the API offers `place-bid` and the user is a buyer; a refused bid shows the server's reason and the new state.
- *Tests:* `AuctionBiddingTest`, `PlaceBidUseCaseTest`, `BidsApiTest`, `usePlaceBid.spec.ts`

### US4 — Two buyers bid at the same time
- Exactly one of two simultaneous bids is accepted; the other gets 409 `concurrent-modification` and can retry.
- The stored highest bid and the stored bids are consistent (no lost update).
- *Tests:* `ConcurrentBiddingTest`, `AuctionPersistenceAdapterTest.staleCopyCannotOverwriteANewerVersion`, `ConcurrentBidConflictApiTest`

### US5 — As a logged-in user, I see an auction's details and bids
- The detail and bid list need a valid token (401 otherwise); an unknown auction → 404.
- The detail shows the car, prices, dates and status, with `self` and `bids` links, plus `place-bid` while open.
- *Tests:* `QueryAuctionsApiTest`, `BidsApiTest`, `auction.spec.ts`

### US6 — An auction is closed and awarded
- Closing a RUNNING auction makes it ENDED and sets the winner to the highest bidder (no winner if there were no bids).
- A closed or cancelled auction can't be closed again and accepts no more bids.
- *Tests:* `AuctionClosingTest`, `CloseAuctionUseCaseTest`
- ⚠️ Nothing triggers closing yet (see *What was skipped*).

### US7 — I log in with my company account
- Login goes through Keycloak (code flow + PKCE); the roles come from the token.
- My profile (name, email, phone, role) is taken from the token and kept in sync; a token without exactly one auction role or without a phone number → 403 `incomplete-identity`.
- Invalid, forged or expired tokens → 401 problem.
- *Tests:* `SecurityApiTest`, `KeycloakRealmRoleConverterTest`, `ProvisionUserUseCaseTest`, `auth.spec.ts`

---

## What was skipped, and why

| Skipped | Why / what it would take |
|---|---|
| **Automatic closing at end date** | The rule and use case (`CloseAuction`) exist and are tested, but no scheduler or endpoint calls them. Until then, auctions past their end stay RUNNING in the database. They're hidden from the running list and refuse bids, so nothing incorrect is exposed. Next step: a scheduled job that closes expired auctions, with a retry when the version check reports a conflict. |
| Cancelling an auction via the API | `Auction.cancel()` exists in the domain, but who may cancel (the seller? only before the first bid?) is a product decision that wasn't made. |
| Registration and user management | Keycloak owns the accounts; the app only mirrors the profile from the token. |
| Notifications, payments, winner contact | Outside the MVP's core (the auction rules and concurrency). |
| Car photos | Needs file storage; not core to the bidding rules. |
| Unique email in `users` | With soft delete it requires a partial unique index (`WHERE deleted = false`), which is PostgreSQL-only, while tests also run on H2. Keycloak already guarantees unique emails. |
| Race on a brand-new user's first two simultaneous requests | Both could try to create the user; unlikely in practice and not handled yet. |
| Currency | One implicit currency; the UI shows EUR. |
| Production setup | Keycloak runs in dev mode with a password grant enabled for testing, images aren't pushed to a registry, and there's no TLS/hosting config. |
| UI component tests, browser end-to-end tests | The tests cover the store, composables and HTTP client. The real Keycloak login was checked manually through the stack, not automated. |
| Latest frontend tool majors | Node 20.18 (the local version) limits the stack to Vite 6 / Vitest 3 / vue-router 4 / Pinia 3. Upgrading Node to 22 allows the newer majors. |

---

## How the agent was used

The project was built with **Claude Code**, one prompt per step, following the order in [`PROMPTS.md`](PROMPTS.md):

1. **A harness first.** The first commit added [`CLAUDE.md`](CLAUDE.md). It holds the stack, the hexagonal/DDD boundaries, CQRS, soft delete only, TDD, Conventional Commits with one small commit per step and never on a red build, and "`mvn verify` before saying it's done". Claude Code loads it in every session, so these rules apply to every step without being repeated.
2. **One small step per prompt:** scaffold → domain model (TDD) → ArchUnit → use cases → JPA + locking → REST → security → frontend → Docker/CI → migrations check. Each prompt was small enough to review.
3. **Test first, every time.** For each step the agent wrote the tests, ran them to see them fail, implemented, ran `mvn verify` (and `npm test` / `npm run build`), then committed. The history is 40+ small commits; none was made on a red build.
4. **Rules made machine-checkable,** so they don't depend on the agent remembering them: the Maven Enforcer rule (domain without frameworks), ArchUnit (layers, CQRS separation), `ddl-auto=validate` plus `SchemaMigrationTest`, and the CI workflow.
5. **Verification beyond unit tests.** Where mocks could hide problems, the agent ran the real thing:
   - the jar against PostgreSQL;
   - real Keycloak tokens, including forged ones;
   - the Vite proxy and the HAL links;
   - the whole compose stack.
6. **Review after each step.** Every response ended with *decisions to review* (choices not in the prompt), and each step was validated before the next. [`PROMPTS.md`](PROMPTS.md) logs every prompt verbatim with a summary of the response and its validation status. `CLAUDE.md` was kept up to date as the project grew, so later steps followed the conventions set earlier.

What this caught along the way:
- **A lost-update bug:** a stale auction could overwrite a newer bid. The concurrency test exposed it before any code shipped.
- **Lombok in the domain:** ArchUnit showed that Lombok leaves a compile-time-only annotation in the domain bytecode, so that dependency was made explicit.
- **A misleading test:** the first tampered-token check changed a meaningless base64 padding character, so a bad request still got 200. It was replaced with real signature and payload forgeries, which correctly get 401.
