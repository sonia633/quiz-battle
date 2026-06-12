# Java Microservices

Spring Boot 3.3 / Java 21 services for the AI Quiz Battle Platform.

| Service | Port | Responsibility |
|---------|------|----------------|
| `api-gateway` | 8080 | Edge routing, JWT validation, Redis rate limiting, CORS |
| `quiz-service` | 8081 | Auth (JWT + refresh rotation), users, categories, quizzes, questions, multiplayer rooms (WebSocket/STOMP), achievements |
| `leaderboard-service` | 8082 | Result ingestion, global/weekly/monthly rankings, XP, player statistics |
| `tournament-service` | 8083 | Tournaments, single-elimination bracket scheduling, rewards, notifications |

## Architecture notes

- **Edge auth.** The gateway validates the HS256 access token and forwards
  `X-User-Id` / `X-User-Name` / `X-User-Roles` to downstream services. The
  `quiz-service` also validates the token itself (defence in depth); the
  leaderboard and tournament services trust the gateway-supplied headers.
- **Shared secret.** `JWT_SECRET` must match between `api-gateway` and
  `quiz-service` so tokens minted by quiz-service validate at the edge.
- **Shared database.** All services target one PostgreSQL database
  (`quizbattle`). Each service owns its own tables; the canonical schema lives
  in the `database/` module. In dev, `JPA_DDL_AUTO=update` lets each service
  create its tables; in production set it to `validate`.
- **Cross-service flow.** When a battle finishes, `quiz-service` persists
  `game_results` and best-effort POSTs them to `leaderboard-service`
  (`/api/stats/results`). Tournament rewards are awarded the same way.

## Build & run (local)

Requires JDK 21. Each service is an independent Maven project:

```bash
cd quiz-service && ./mvnw spring-boot:run     # or: mvn spring-boot:run
```

Or run everything via Docker — see the repository-root `docker/` module
(`docker compose up -d`), which builds each service in a container (no local JDK
required).

## Test

```bash
mvn test        # in any service directory
```

- `quiz-service`: `AuthServiceTest` (Mockito), `GameSessionTest` (scoring),
  `AuthFlowIntegrationTest` (full HTTP flow on H2).
- `leaderboard-service`: `LeaderboardServiceTest` (ingestion + ranking).
- `tournament-service`: `BracketSeedingTest`, `TournamentFlowTest`
  (end-to-end 4-player bracket).

## API docs

Each service exposes Swagger UI at `/swagger-ui.html` and the OpenAPI spec at
`/v3/api-docs`.
