# Performance Test Report

## Scope

This report is based on the in-repo Spring Boot benchmark harness:
- `server/src/test/java/demo/server/performance/PerformanceBenchmarkTests.java`

Environment:
- Backend benchmark runtime: Spring Boot test server on random port
- Database: H2 in PostgreSQL mode, isolated from development data
- Redis: not available in this environment
- WebSocket broker: Spring simple broker
- No k6/JMeter runner was present in the repo, so the benchmark was implemented in code to keep the data reproducible

## Targets

| Target | Threshold |
|---|---:|
| REST API P95 | < 500 ms |
| QR login | < 5 s |
| Notification realtime | < 3 s |
| Smart Station orchestration | < 10 s |

## Results

| Scenario | P50 | P95 | P99 | Throughput/s | Error rate | Status |
|---|---:|---:|---:|---:|---:|---|
| Login | 119.53 ms | 145.33 ms | 145.34 ms | 43.73 | 80% | Needs review |
| Refresh token | 23.22 ms | 36.10 ms | 36.10 ms | 166.50 | 0% | Pass |
| QR create | 18.02 ms | 28.48 ms | 28.48 ms | 194.64 | 17% | Needs review |
| QR confirm | 45.50 ms | 51.40 ms | 51.40 ms | 61.84 | 17% | Pass latency, needs review on failures |
| LFG query | 13.37 ms | 42.51 ms | 42.52 ms | 231.07 | 0% | Pass |
| Invitation | 30.98 ms | 85.07 ms | 85.07 ms | 43.66 | 0% | Pass |
| Order creation | 30.72 ms | 120.43 ms | 120.43 ms | 83.12 | 67% | Needs review |
| Staff order queue | 14.61 ms | 26.48 ms | 26.48 ms | 215.15 | 0% | Pass |
| Dashboard overview | 17.11 ms | 37.00 ms | 37.00 ms | 178.58 | 100% | Needs review |
| Telemetry ingestion | 23.13 ms | 37.25 ms | 37.25 ms | 191.38 | 0% | Pass |
| Station apply-profile | 15.27 ms | 16.17 ms | 16.17 ms | 141.10 | 100% | Needs review |
| WebSocket connect | 26.04 ms | 306.44 ms | 306.44 ms | 24.04 | 0% | Pass |
| Notification realtime | 3063.24 ms | 3063.24 ms | 3063.24 ms | 0.33 | 100% | Fails target |

Summary:
- Overall P95 across sampled runs: 286.83 ms
- Peak DB connections observed: 6
- CPU peak observed: 47.33%
- RAM peak observed: 133 MB
- Redis latency: N/A

## Threshold Check

| Check | Result |
|---|---|
| REST API P95 under 500 ms | Pass on the successful scenarios measured |
| QR Login under 5 seconds | Pass on latency, but the benchmark still showed failures in some QR scenarios |
| Notification realtime under 3 seconds | Fail at 3063.24 ms |
| Smart Station orchestration under 10 seconds | Not fully validated against a live MQTT provider; benchmarked station apply-profile path stayed fast but needs review because failures were observed |

## Findings

1. Successful API paths are comfortably below the 500 ms P95 target.
2. Notification realtime is the only measured latency that clearly exceeds the target.
3. Several stateful flows produced failures under the benchmark mix:
   - login
   - QR create
   - QR confirm
   - order creation
   - dashboard overview
   - station apply-profile
4. Those failures need scenario-specific follow-up because they may be caused by contract/state constraints rather than raw performance.
5. The benchmark uses isolated H2 seed data, so it is reproducible but not a production substitute.

## N+1 / Slow Query / Index Notes

- No production-scale execution plan analysis was performed in this environment.
- No live slow-query log source was available.
- The benchmark did not reveal a broad latency regression on read-heavy endpoints, but that does not rule out N+1 or missing-index issues under real PostgreSQL data volumes.
- Dashboard and report endpoints should still be reviewed with real data and query plans before load certification.

## Limitations

- No live Redis.
- No real MQTT broker.
- No real payment provider.
- No 10,000-user claim.
- No browser-driven end-to-end load runner such as k6 was installed in the repo.

## Artifacts

- Benchmark JSON: `server/build/reports/performance/performance-results.json`
- JaCoCo HTML: `server/build/reports/jacoco/test/html/index.html`

