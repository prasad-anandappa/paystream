# BUILD-LOG — PayStream flagship

Record of how this project was built and verified on 2026-11-02. Claims here are
from the actual build run, not aspirations.

## Environment (probed, not assumed)

| Fact | Value |
|---|---|
| JDK | OpenJDK Temurin 21.0.13 (`~/workspace/tools/jdk-21`) |
| Maven | 3.9.9 (`~/workspace/tools/apache-maven-3.9.9`) |
| Spring Boot | 3.4.0 (matches the P1 ledger project) |
| Docker | **UNAVAILABLE** in this sandbox (`docker info` fails) |
| Maven Central | Reachable (HTTP 200); local `~/.m2` started empty |
| Disk free | ~40 GB — sufficient for dependency downloads |

## Key decision: test database

The brief said "Testcontainers if Docker works here, else H2 fallback (decide by
actually probing Docker)". Docker was probed and is **not available**, so the
test strategy is:

- All module + API tests run on **H2 in PostgreSQL-compatibility mode**
  (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`).
- The **same Flyway migrations** (portable SQL, identity columns, standard types)
  build both the H2 test schema and the real PostgreSQL 16 schema.
- `docker-compose.yml` ships PostgreSQL 16 for local development, and CI
  validates the compose file (`docker compose config --quiet`) even though the
  test suite itself needs no database service.

Consequence: Testcontainers is intentionally NOT a dependency. If the project is
later built somewhere with Docker, adding a Testcontainers IT profile is a
pom-only change — the migrations and JPA mappings are already portable.

## Verification — `mvn -B verify` (first full run)

```
PayStream .......................................... SUCCESS
PayStream :: Core .................................. SUCCESS   Tests run: 7
PayStream :: Events ................................ SUCCESS   Tests run: 2
PayStream :: Reconciliation ........................ SUCCESS   Tests run: 2
PayStream :: API ................................... SUCCESS   Tests run: 3
BUILD SUCCESS — Total time: 01:31 min — 2026-11-02
```

**14 tests run, 0 failures, 0 errors, 0 skipped**, across:

- `LedgerServiceTest` (3) — balanced entry moves balances; unbalanced entry
  rejected with nothing persisted; non-positive amounts rejected.
- `TransferServiceTest` (4) — single application of funds; idempotency-key
  replay returns the original transfer and moves nothing; insufficient funds;
  self-transfer rejection.
- `OutboxFlowTest` (2) — transfer writes a PENDING `transfer.completed` outbox
  event; relay publishes it once, marks it PUBLISHED, second pass is a no-op.
- `ReconciliationServiceTest` (2) — healthy books balanced with equal trial
  totals (12,250 minor units each side); a cached balance tampered by 1 minor
  unit is caught, counted and named ("MISMATCH") in the persisted report.
- `PayStreamApiTest` (3) — full HTTP flow against the seeded `SYSTEM_CASH`
  account: deposit 10,000 → pay 2,500 → replay same key returns 200 with no
  second movement → balances 7,500 / 2,500 / −10,000 → reconciliation report
  balanced; overdraft attempt returns 422; unknown account returns 404.

The API module also repackages into a runnable Spring Boot jar
(`api/target/paystream-api-0.1.0-SNAPSHOT.jar`).

## Not verified (honest list)

- **Live PostgreSQL run**: Docker is unavailable here, so the app was not
  booted against a real PostgreSQL 16 in this environment. The schema path is
  verified via the identical Flyway migrations on H2 PostgreSQL-mode, and
  `flyway-database-postgresql` is on the runtime classpath. First local run
  with `docker compose up -d` closes this gap.
- **README links**: the YouTube/Instagram channel URLs in the README footer
  should be re-verified against the live channel pages before this repo is
  pushed (carried over from the promo-plan checklist).

## Push status

Built locally only, per instructions. The parent agent verifies and pushes to
`github.com/prasad-anandappa`.
