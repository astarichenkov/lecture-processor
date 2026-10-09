# Day 30 — test results

## Executed automated tests

Command: `./mvnw -pl private-ai-chat test`

| Test class | Tests | Result |
|-----------|-------|--------|
| PrivateAiChatApplicationTests (context) | 1 | PASS |
| ChatServiceTest | 5 | PASS |
| ChatMemoryStoreTest | 2 | PASS |
| RateLimiterTest | 2 | PASS |
| ApiKeyInterceptorTest | 4 | PASS |
| ChatApiControllerTest | 5 | PASS |
| **Total** | **19** | **PASS** |

Covered: input validation, context trimming, bounded queue/metrics, LLM failure
propagation, per-client rate limiting, API-key auth (missing/header/bearer),
health/chat endpoints, 400/429/503 responses.

## Stability checks (protocol)

| # | Check | Automated | Real run |
|---|-------|-----------|----------|
| 1 | Model responds | SKIPPED | depends on Ollama |
| 2 | HTTP API works | PASS (MockMvc) | SKIPPED |
| 3 | Follow-up preserves context | PASS (unit) | SKIPPED |
| 4 | Sequential requests | PASS (unit) | SKIPPED |
| 5 | Concurrent requests | NOT IMPLEMENTED (single worker by design) | — |
| 6 | Rate limiting | PASS | SKIPPED |
| 7 | Oversized rejected | PASS | SKIPPED |
| 8 | Unauthenticated rejected | PASS | SKIPPED |
| 9 | Recovery from model timeout | PASS (timeout path) | SKIPPED |

## Homelab deployment

Status: **SKIPPED** — no access to the homelab server from this environment.
Deployment scripts and Compose file are provided and documented; no successful
remote deployment is claimed.
