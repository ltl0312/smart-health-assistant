# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build & Run

```bash
# Backend
cd health-assistant-backend
mvn spring-boot:run                        # dev on localhost:8080
mvn test                                   # run all tests (13 tests, JUnit 5 + Mockito)
mvn test -Dtest=ClassName                  # run single test class
mvn compile                                # compile only
mvn clean package -Dmaven.test.skip=true   # build JAR

# Frontend
cd health-assistant-frontend
npm run dev                                # Vite dev server on :5173
npm run build                              # production build → dist/
npm run test                                # Vitest

# Docker (full stack)
docker compose up -d --build               # deploy all 4 services
docker compose up -d --build backend       # rebuild backend only
docker compose logs backend --tail 50      # check logs
docker compose ps                          # status
```

## Architecture

**Backend**: Spring Boot 3.2.5 / Java 17 / MyBatis-Plus / MySQL 8.0 / Redis 7 / JWT auth.
**Frontend**: Vue 3 SPA (no Vue Router — view-switching via `activeView` ref) / Tailwind CSS with CSS variables for theming / Chart.js / Pinia stores.
**AI**: DeepSeek V4 Pro via `RestTemplate`, with `@Retryable` (3 attempts, 2s backoff). Chat via `POST /api/chat/message`, plan generation stores `diet_plan_json` and `workout_plan_json` in `ai_plan` table.

### Key Design Decisions

1. **No Spring Security** — JWT via custom `AuthInterceptor` + `@RequireRole` AOP annotation for ADMIN endpoints.
2. **No Vue Router** — Single-page view switching persisted to `localStorage`. Login gate in `App.vue`.
3. **Theming via CSS variables** — All Tailwind colors map to `var(--color-xxx)`. `:root` = light, `html.dark` = dark. Do NOT add `dark:` Tailwind variants — use CSS variables instead.
4. **MarkdownGenerator** must handle 5+ DeepSeek JSON output formats — see `memory/debugging-patterns.md` item 5.
5. **`application.yml` is gitignored** — use `application.example.yml` as template. Keys via env vars with `${VAR:fallback}`.

### Backend Packages

| Package | Purpose |
|---------|---------|
| `model/` | MyBatis-Plus entities: SysUser, HealthProfile, WeightRecord, AiPlan |
| `mapper/` | `BaseMapper<T>` interfaces (WeightRecordMapper has custom `@Select` queries) |
| `service/impl/` | Business logic: Auth, Profile, Weight (BMI calc), AiPlan (DeepSeek call), Chat (conversation), Rank (leaderboard) |
| `controller/` | REST endpoints (see table below) |
| `security/` | JwtUtil (generate/validate), PasswordEncoder (BCrypt) |
| `interceptor/` | AuthInterceptor — validates Bearer token, injects `userId` + `role` |
| `aop/` | RoleCheckAspect — enforces `@RequireRole("ADMIN")` |
| `config/` | CorsConfig, WebMvcConfig (interceptor + static resources), DeepSeekConfig, RetryConfig |
| `util/` | BmiCalculator, WeightTrendAnalyzer, PromptBuilder, MarkdownGenerator, PasswordResetService |
| `utils/` | PromptCleaner (regex strip markdown code blocks) |
| `dto/` | Request/Response objects |
| `exception/` | BusinessException + GlobalExceptionHandler (`@RestControllerAdvice`) |

### API Endpoints

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/auth/register` | No | Username+password+email |
| POST | `/api/auth/login` | No | Returns JWT + hasProfile + role |
| GET/PUT | `/api/user/profile` | JWT | Nickname/phone/email/bio |
| PUT | `/api/user/password` | JWT | Old + new password |
| POST | `/api/user/avatar` | JWT | Multipart upload |
| DELETE | `/api/user/account` | JWT | Soft-delete (status=0) |
| GET/POST | `/api/profile` | JWT | Health profile CRUD |
| PUT | `/api/profile/height` | JWT | Weekly limit 3x (resets Monday) |
| GET | `/api/profile/export` | JWT | Markdown export |
| POST/PUT | `/api/weight/record` | JWT | BMI auto-calc, daily edit limit 2x |
| GET | `/api/weight/history?days=30` | JWT | Sorted ASC by date |
| POST | `/api/plan/generate` | JWT | Direct DeepSeek call |
| POST | `/api/chat/message` | JWT | Conversational AI with context |
| GET | `/api/rank/health?period=weekly\|monthly` | No | Redis cached 30min |
| GET/DELETE | `/api/records` `/{id}` | JWT | List (3mo), download .md, delete |
| GET | `/api/quote/health` | JWT | Daily health quote |
| GET | `/api/admin/stats` `/users` | JWT+ADMIN | System stats |

### Weight Trend Calculation

`WeightTrendAnalyzer.analyze()` — Sorts records by date ASC, computes delta between first and last. Must create a mutable copy before sorting because input may be `List.of()` (immutable). The caller in `AiPlanServiceImpl` passes `selectRecentByUserId(limit=4)` which returns DESC — sorting fixes this.

### Health Score Formula (Rank)

`100 + consecutiveWeeks×2 - |BMI-22|×5 + goalBonus` (capped 0-200). Redis cache key = `healthRank::{period}`.

### Downloads / Blob Handling

`request.js` response interceptor checks `response.config.responseType === 'blob'` and passes through directly — otherwise the `res.code !== 200` check fails on binary data. Frontend download must use `axios.get({ responseType: 'blob' })` + `URL.createObjectURL(new Blob([result]))`.

### Common Pitfalls

- **DB `created_at`**: Remove `@TableField(fill = FieldFill.INSERT)` from models — let MySQL `DEFAULT CURRENT_TIMESTAMP` handle it.
- **JSON columns in MySQL**: Must store valid JSON strings. Use Jackson `writeValueAsString()`, never raw strings.
- **Immutable lists**: `List.of()` throws `UnsupportedOperationException` on `.sort()`. Copy first.
- **Windows curl + Chinese**: Sends GBK, causes `Invalid UTF-8 middle byte` on backend. Use English or test from browser.
- **Time display**: MySQL stores UTC. Frontend: `new Date(dt + 'Z')` to get local time.
- **`application.yml` is gitignored** — always check `application.example.yml` for config structure.
