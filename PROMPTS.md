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

**Status:** ⏳ Waiting for your validation

---
