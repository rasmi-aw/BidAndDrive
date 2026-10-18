# Prompt Log — auction_mvp

A record of every prompt given to Claude Code in this project and the response to it.
An entry is added once the prompt's work is done **and validated by the user**.

---

## Prompt 1 — Set up the prompt log

**Date:** 2026-10-18

### Prompt

> all the conversation and responses well markedup should be put in a file inside the root called PROMPTS.md that should be updated after each prompt accomplished and validated by me

### Response

- Created `PROMPTS.md` in the project root (this file).
- Saved this as a standing rule for the project: after each prompt is done and you validate it, a new entry is added here with the prompt (quoted exactly) and a well-formatted summary of the response, including files changed and key decisions.

**Status:** ✅ Validated

---

## Prompt 2 — Create CLAUDE.md with project context

**Date:** 2026-10-18

### Prompt

> create a CLAUDE.md at the root with the project context:
> - monorepo: backend/ (java 25, spring boot 3.5, maven multi module: auction-domain, auction-infrastructure, auction-app), frontend/ (vue 3 + ts + vite), docker-compose.yml at root
> - hexagonal + DDD: domain module has zero spring/jpa deps, only pure java. ports in domain, adapters in infra, app = controllers + config
> - CQRS light: separate command and query use cases
> - TDD: write the test first, then the code
> - soft delete only, no hard delete
> - conventional commits, one small commit per step, never commit if build is red
> - always run mvn verify before saying its done

### Response

- Created `CLAUDE.md` in the project root with these sections:
  - **Repository layout**: tree of `backend/` (the 3 Maven modules), `frontend/`, `docker-compose.yml`.
  - **Architecture**: hexagonal + DDD module responsibilities and dependency direction (`app → infrastructure → domain`, domain depends on nothing); CQRS light; soft-delete-only rule.
  - **Workflow**: TDD cycle, Conventional Commits with small per-step commits and no commits on a red build, and `mvn verify` (run from `backend/`) as the definition of done.
- Additions beyond the prompt: JPA entities stay in infrastructure and are mapped to domain objects; domain tests run without a Spring context. These are worth checking.

**Status:** ✅ Validated

---

## Prompt 3 — Scaffold the Maven multi-module backend

**Date:** 2026-10-18

### Prompt

> scaffold the maven multi module project in backend/ with the 3 modules, parent pom manages versions.
> deps: lombok, validation, data jpa, postgresql, h2 (test), springdoc-openapi, junit5, mockito, archunit, testcontainers.
> domain module must not depend on spring. just an empty app that starts + one context test. build it.

### Response

- **Parent `backend/pom.xml`** (`com.auction:auction-parent`): inherits `spring-boot-starter-parent` **3.5.16** (latest 3.5.x), Java 25.
  - The Boot BOM manages Lombok, JUnit 5, Mockito, H2, PostgreSQL and Testcontainers; the parent adds `springdoc-openapi` 2.8.17, `archunit-junit5` 1.4.2 and the internal modules to `dependencyManagement`.
  - Lombok is on the annotation processor path (JDK 23+ no longer picks processors up from the classpath).
  - Mockito is loaded as a `-javaagent` in Surefire (removes the JDK self-attach warning).
- **`auction-domain`**: no runtime dependencies; tests only use JUnit, AssertJ and Mockito. A `maven-enforcer` rule **fails the build** if Spring, `jakarta.persistence` or Hibernate show up. Checked by adding `spring-context` temporarily: the build failed as expected.
- **`auction-infrastructure`**: domain, Data JPA, PostgreSQL (runtime); tests use H2 and Testcontainers (PostgreSQL + JUnit 5).
- **`auction-app`**: domain + infrastructure, Web, Validation, springdoc; tests use H2 and ArchUnit. Contains `AuctionApplication`, `application.yml` (PostgreSQL through `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars) and `AuctionApplicationTest` (context test on H2 with the `test` profile).
- **Verified:** `mvn verify` → BUILD SUCCESS (1 test). The jar started against a temporary PostgreSQL 17 container; `/v3/api-docs` and `/swagger-ui.html` returned 200.
- Not done: no git repo yet, so nothing committed; no `docker-compose.yml` yet.

**Status:** ✅ Validated

---

## Prompt 4 — TDD the domain model

**Date:** 2026-10-18

### Prompt

> in auction-domain, TDD the model. tests first.
> User: id, fullName, email, phone (all required), role SELLER/BUYER, createdAt, updatedAt
> Auction: carName, condition GOOD/BAD/EXCELLENT, yearMade, yearRegistered, description, startPrice, currentHighestBid, startDate, endDate, status RUNNING/CANCELLED/ENDED, wonByUserId, version
> Bid: auctionId, userId, givenPrice, createdAt
> rules:
> - only SELLER creates an auction
> - only BUYER bids, seller cant bid on his own auction
> - bid > max(startPrice, currentHighestBid)
> - no bid if not RUNNING or endDate passed
> - endDate > startDate, yearRegistered >= yearMade, startPrice > 0
> - close auction -> ENDED + wonByUserId = highest bidder
> use value objects for money/email/phone, domain exceptions, no setters. inject a Clock for time.

### Response

Built in 5 TDD steps. Each step: tests written first and seen failing, then implemented, `mvn verify` green, one commit.

| Commit | Content |
|---|---|
| `7738ad4` | `Money` (non-negative, scale 2), `DomainException`, `DomainValidationException`, `Guard` |
| `3ab8113` | `User.register(...)`, `Email`, `Phone`, `Role`, `UserId` |
| `8dfe200` | `Auction.create(...)`, `CarDetails`, `CarCondition`, `AuctionStatus`, `AuctionId` |
| `5fdbc55` | `Auction.placeBid(...)` → `Bid`, `BidId`, bidding exceptions |
| `8b1c040` | `Auction.close()`, `Auction.cancel()`, `AuctionNotRunningException` |

- **Packages:** `com.auction.domain.shared`, `.user`, `.auction`.
- **Domain exceptions** (all extend `DomainException`): `DomainValidationException`, `OnlySellerCanCreateAuctionException`, `OnlyBuyerCanBidException`, `SellerCannotBidOnOwnAuctionException`, `BidTooLowException`, `AuctionNotOpenForBiddingException`, `AuctionNotRunningException`.
- **Clock:** passed to `User.register` and `Auction.placeBid` (the domain has no DI).
- **Decisions to review:**
  - Fields added beyond the spec: `Auction.id`, `sellerId` (needed for the own-auction rule), `highestBidderId` (needed for close), `Bid.id`.
  - Car fields grouped in a `CarDetails` value object (`auction.getCar().carName()`); `description` is optional.
  - Bidding window is `[startDate, endDate)`: bids before `startDate` or exactly at `endDate` are rejected.
  - `close()` only requires RUNNING; it does not check that `endDate` has passed.
  - Added `cancel()` (RUNNING → CANCELLED) so the CANCELLED status is reachable.
  - No `restore(...)` factories yet for loading from the DB; to be added with the persistence adapter.
- **Verified:** `mvn verify` → BUILD SUCCESS, 68 domain tests + 1 app context test. The domain module's only non-test dependency is Lombok (`provided`).

**Status:** ✅ Validated

---

## Prompt 5 — ArchUnit boundary tests

**Date:** 2026-10-18

### Prompt

> add archunit tests in auction-app to enforce: domain depends on nothing (no spring, no jakarta.persistence), infra doesnt depend on app, controllers dont touch repositories directly. make them pass.

### Response

- Added `auction-app/src/test/java/com/auction/app/architecture/ArchitectureTest.java` (commit `79eda0f`). It scans all production classes under `com.auction` and has 4 rules:
  1. `domain_does_not_depend_on_spring_or_jpa`: no `org.springframework..`, `jakarta.persistence..` or `org.hibernate..`.
  2. `domain_depends_only_on_the_jdk_and_itself`: only `java..` and `com.auction.domain..`, plus the root `lombok` package.
  3. `infrastructure_does_not_depend_on_app`.
  4. `controllers_do_not_use_repositories`: `@RestController` / `@Controller` classes must not depend on Spring Data `Repository` types, `@Repository` beans, or anything named `*Repository` (which covers domain repository ports).
- **Finding:** rule 2 first failed 19 times. Lombok puts `@lombok.Generated` on the getters and `equals`/`hashCode` it generates in `User` and `Auction`. The annotation is compile-time only and Lombok stays `provided`, so the root `lombok` package is allowed and the reason is written as a comment in the test.
- Rules 3 and 4 use `allowEmptyShould(true)` because there are no infrastructure classes or controllers yet.
- **Proved the rules work:** temporary classes that broke every rule made all 4 tests fail with the right violations; then they were deleted.
- `CLAUDE.md`: noted the architecture rules and how to build a single module (`-am`).
- **Verified:** `mvn clean verify` → BUILD SUCCESS (68 domain + 5 app tests).

**Status:** ✅ Validated

---

## Prompt 6 — Application layer (CQRS use cases)

**Date:** 2026-10-18

### Prompt

> add the application layer: commands CreateAuction, PlaceBid, CloseAuction and query ListRunningAuctions (paged).
> define repository ports in domain. unit test use cases with mockito.

### Response

**Where:** use cases live in `auction-domain` under `com.auction.application.command` / `.query`. They can't use Spring, and this is the only pure-Java module. `auction-app` will wire them as beans once adapters exist.

| Commit | Content |
|---|---|
| `5f3f6dd` | Ports in domain: `AuctionRepository` (save, findById), `BidRepository` (save), `UserRepository` (findById), read port `AuctionReadRepository.findRunning(now, PageQuery)` → `Page<AuctionSummary>`; `PageQuery` (0-based, size 1..100), `Page`; `AuctionNotFoundException`, `UserNotFoundException` |
| `d72b0ca` | `CreateAuctionUseCase.handle(CreateAuctionCommand)` → `AuctionId` |
| `806a0f3` | `PlaceBidUseCase.handle(PlaceBidCommand)` → `BidId`; saves the auction, then the bid |
| `b9b7953` | `CloseAuctionUseCase.handle(CloseAuctionCommand)` |
| `c7394bc` | `ListRunningAuctionsUseCase.handle(ListRunningAuctionsQuery)` → `Page<AuctionSummary>` |
| `c019f70` | ArchUnit: application depends only on domain + JDK; commands and queries don't depend on each other |

- **Tests:** Mockito (`@ExtendWith(MockitoExtension.class)`, strict stubs). Each use case has a happy path, not-found paths, a domain rejection (verifies nothing is saved), and command/query argument checks.
- **Decisions to review:**
  - Commands return only an id (or nothing); the query uses a separate read port and read model.
  - `ListRunningAuctions` means status RUNNING **and** `now` inside `[startDate, endDate)`, ordered by `endDate` ascending (stated in the port's contract; the adapter must implement it).
  - `PlaceBid` saves the auction before the bid, so a version conflict fails before the bid is written.
  - **Not done yet:** transactions and Spring wiring (no adapters yet, so beans can't be created); mapping optimistic-lock failures to a domain error.
- New ArchUnit rules checked against throwaway violating classes: all 3 failed as expected; the classes were then removed.
- **Verified:** `mvn clean verify` → BUILD SUCCESS: 92 tests in the domain module (model + use cases) and 8 in the app module (context test + 7 architecture rules).

**Status:** ⏳ Waiting for your validation

---
