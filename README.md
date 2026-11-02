# PayStream — Event-Driven Payments Platform

A small but honest payments platform in **Java 21 / Spring Boot 3**: a double-entry
ledger at the core, idempotent transfers, a transactional outbox for domain events,
and scheduled reconciliation that independently re-audits the books. Built to
production *patterns* — no money is ever a `double`, no transfer can be applied
twice, and no balance is ever trusted without proof.

![CI](https://github.com/prasad-anandappa/paystream/actions/workflows/ci.yml/badge.svg)

---

## Why this exists

Most demo "payment apps" are a balance column and an `UPDATE` statement. Real
payment systems are built differently, and this project demonstrates the
difference:

- **Double-entry bookkeeping** — every movement of money is a journal entry whose
  debits equal its credits. Money is never created or destroyed inside the system.
- **Idempotency keys** — a retried payment request returns the original transfer
  instead of charging twice. (201 the first time, 200 on replay.)
- **Transactional outbox** — the `transfer.completed` event is written in the
  *same database transaction* as the transfer, then relayed by a polling
  publisher. State and events can never disagree.
- **Reconciliation as a control** — a scheduled job recomputes every account
  balance from the posting history and checks the platform-wide trial balance.
  If a cached balance ever drifts, the report names the account.
- **Minor units only** — amounts are `long` cents end to end. Floating point
  never touches money.

## Architecture

```mermaid
flowchart LR
    Client([API client]) -->|REST| API

    subgraph API["paystream-api — runnable Spring Boot app"]
        API[Controllers<br/>accounts · transfers · reconciliation]
        SCHED[Recon scheduler<br/>every 5 min]
    end

    subgraph CORE["paystream-core — the ledger"]
        LEDGER[LedgerService<br/>balanced-entry invariant]
        TRANSFER[TransferService<br/>idempotency keys]
        PORT{{DomainEventPublisher port}}
    end

    subgraph EVENTS["paystream-events — transactional outbox"]
        ADAPTER[Outbox adapter]
        RELAY[Polling relay<br/>at-least-once]
        TRANSPORT[EventTransport<br/>logging · Kafka-ready]
    end

    subgraph RECON["paystream-recon — the auditor"]
        RECON[ReconciliationService<br/>recompute + trial balance]
    end

    API --> LEDGER
    API --> TRANSFER
    API --> RECON
    SCHED --> RECON
    TRANSFER --> LEDGER
    TRANSFER --> PORT
    PORT --> ADAPTER
    ADAPTER --> DB[(PostgreSQL<br/>Flyway-managed schema)]
    LEDGER --> DB
    RECON --> DB
    RELAY --> DB
    RELAY --> TRANSPORT
```

Modules depend in one direction only: `api → {core, events, recon}`, `events → core`,
`recon → core`. The core never depends on delivery technology — it announces events
through a port (hexagonal style), and the outbox module provides the adapter.

## Quickstart

**Option A — no Docker (embedded database, 30 seconds):**

```bash
mvn -q -DskipTests package
java -jar api/target/paystream-api-0.1.0-SNAPSHOT.jar --spring.profiles.active=h2
```

**Option B — real PostgreSQL 16 via Docker Compose:**

```bash
docker compose up -d          # starts PostgreSQL with the app's dev credentials
mvn -q -DskipTests package
java -jar api/target/paystream-api-0.1.0-SNAPSHOT.jar
```

Then move some money:

```bash
# Create two accounts (note the seeded SYSTEM_CASH account, id from GET /api/accounts)
curl -X POST localhost:8080/api/accounts -H 'Content-Type: application/json' \
  -d '{"name":"Alice","type":"LIABILITY","currency":"USD","allowNegative":false}'

# Deposit $100.00 (amounts are minor units) from SYSTEM_CASH into Alice — replay-safe key
curl -X POST localhost:8080/api/transfers -H 'Content-Type: application/json' \
  -d '{"idempotencyKey":"dep-1","fromAccountId":1,"toAccountId":2,"amountMinor":10000,"currency":"USD"}'

# Audit the books
curl -X POST localhost:8080/api/reconciliation/run
```

## API

| Method | Endpoint | Notes |
|---|---|---|
| `POST` | `/api/accounts` | Create an account (name, type, currency, allowNegative) |
| `GET` | `/api/accounts` / `/api/accounts/{id}` | List / fetch; carries the cached balance |
| `POST` | `/api/transfers` | Idempotent: same `idempotencyKey` → original transfer, `200` instead of `201` |
| `GET` | `/api/transfers/{id}` | Fetch one transfer |
| `POST` | `/api/reconciliation/run` | Run reconciliation now; returns the persisted report |
| `GET` | `/api/reconciliation/latest` | Most recent report |

Errors use RFC 7807 problem details: unknown account `404`, insufficient funds
`422`, invalid requests `400`.

## Design decisions (the interesting part)

1. **Debit-positive sign convention.** One rule for every account:
   `balance = sum(debits) − sum(credits)`. A transfer CREDITs the source and
   DEBITs the destination. Uniformity beats per-account-type normal-balance
   rules here because it makes the reconciliation recomputation trivially
   auditable — the same two sums, everywhere.
2. **The system funds itself honestly.** Deposits are ordinary transfers out of
   a seeded `SYSTEM_CASH` account — the one account allowed to go negative.
   The ledger balances from transaction #1; there is no "create money" endpoint.
3. **Idempotency is enforced twice.** The service checks the key first, and a
   database unique constraint is the backstop: if two identical requests race,
   the loser's insert fails and it re-reads the winner's transfer.
4. **Optimistic locking on balances.** `Account.version` turns concurrent
   updates into a visible conflict instead of a silent lost update.
5. **Flyway owns the schema; Hibernate never writes DDL.** Migrations are
   portable SQL: the identical scripts build PostgreSQL 16 locally and the
   H2 (PostgreSQL-mode) test database.
6. **Events behind an interface.** The default `EventTransport` logs;
   a Kafka or webhook transport drops in without touching the core or the relay.

## Testing

`mvn verify` runs **14 tests, all green** (see [BUILD-LOG.md](BUILD-LOG.md)):

- **Core** — balanced-entry invariant, unbalanced rejection, idempotent replay,
  insufficient funds, self-transfer rejection.
- **Events** — a transfer writes a PENDING outbox row; the relay publishes it
  exactly once and marks it PUBLISHED.
- **Recon** — healthy books reconcile clean; a deliberately tampered cached
  balance is caught and named in the report.
- **API** — the full flow over HTTP: seed → deposit → pay → retry → balances →
  reconciliation, plus 422/404 semantics.

The suite runs on H2 in PostgreSQL-compatibility mode so it needs no Docker —
a deliberate decision (documented in BUILD-LOG.md) so the tests run everywhere,
including minimal CI sandboxes. The same Flyway migrations run against real
PostgreSQL in local development.

## Project layout

```
core/    domain (accounts, journal entries, postings, transfers), ledger + transfer
         services, Flyway migrations — the books
events/  transactional outbox: entity, adapter for the core's event port, polling relay
recon/   reconciliation service + persisted reports — the auditor
api/     the runnable app: REST controllers, RFC 7807 errors, recon scheduler
```

## 📄 For my résumé — what this project demonstrates

- Designed and built a **multi-module Java 21 / Spring Boot 3 payments platform**
  implementing **double-entry bookkeeping** with an enforced balanced-entry
  invariant and integer minor-unit money handling.
- Implemented **idempotent payment APIs** (client keys + unique-constraint
  backstop + race-safe replay) returning correct `201`/`200` semantics.
- Applied the **transactional outbox pattern** for reliable event publication
  (at-least-once relay, pluggable transport) using a hexagonal port/adapter split.
- Built a **scheduled reconciliation control** that recomputes balances from
  posting history and verifies the trial balance, persisting auditable reports.
- Owned schema evolution with **Flyway** across PostgreSQL and H2; 14 automated
  tests (unit, module-integration and full HTTP end-to-end) green under
  `mvn verify`, with GitHub Actions CI.

## 🎓 Learn the concepts behind this code

Every pattern in this repo is one I teach in short, plain-English videos and
full courses:

- ▶️ **Namma Codertools (YouTube):** https://www.youtube.com/@nammacodertools
- 📸 **Daily Shorts (Instagram):** https://www.instagram.com/nammacodertools/
- 🎓 **Full courses (Udemy):** Java, Python, LLM engineering, Agentic AI, DevOps
  and Observability — see the courses section of my GitHub profile:
  https://github.com/prasad-anandappa

## License

MIT — see [LICENSE](LICENSE).
