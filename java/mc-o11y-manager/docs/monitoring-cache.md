# 모니터링 메트릭 캐시 - 레퍼런스

mc-o11y-manager 의 메트릭 캐시를 **설정하고, 튜닝하고, 고칠 때** 보는 문서입니다.

> **동작 원리와 설계 의도는 여기 없습니다.** 무엇을 왜 이렇게 만들었는지는
> [`docs/info/cache_architecture.md`](../../../docs/info/cache_architecture.md) 를 먼저 읽으세요.
> 이 문서는 그 위에 얹는 상세 정보만 담습니다.

## 목차

1. [코드 지도](#1-코드-지도)
2. [설정 레퍼런스](#2-설정-레퍼런스)
3. [지표 읽는 법](#3-지표-읽는-법)
4. [청크 캐싱 (기본 off)](#4-청크-캐싱-기본-off)
5. [테스트](#5-테스트)
6. [부록 - 무엇이 어떻게 바뀌었나](#6-부록---무엇이-어떻게-바뀌었나)

---

## 1. 코드 지도

### 요청 경로

```
InfluxDBController.postMetricsByVM
  └─ InfluxDbFacadeService
      └─ InfluxDbServiceImpl.getMetricsByVM              ← 입력 검증 + scope 조건 주입
          └─ MonitoringCacheService.getOrLoad            ← 캐시 판정 (fresh / stale / miss)
              └─ InfluxDbServiceImpl.loadMetricsByVM     ← miss 일 때만
                  ├─ InfluxMetaCache.retentionPolicy     ← 보관정책 (메모이즈)
                  ├─ InfluxMetaCache.exists              ← 자원 존재 확인 (메모이즈)
                  ├─ pickDatabase                        ← range ≥ 1d 면 다운샘플 DB
                  ├─ InfluxQl.buildQuery                 ← InfluxQL 생성
                  └─ ChunkedMetricQueryService.fetch     ← 켜져 있으면 슬라이스, 아니면 단일 쿼리
```

### 파일별 역할

| 파일 | 역할 |
|---|---|
| `service/cache/MonitoringCacheService.java` | 캐시 본체 - SWR, single-flight, 빈 결과 처리, 통계 |
| `service/cache/MonitoringCacheKey.java` | 키 구성과 fresh window 산정 |
| `service/cache/MonitoringCacheWarmScheduler.java` | 예열 세 작업 |
| `service/cache/QueryAccessTracker.java` | 조회 이력 추적 (예열 대상 선정용) |
| `service/cache/VmCreatedTimeResolver.java` | 노드 생성시각 조회 (예열 폴백 정렬용, 1시간 메모이즈) |
| `service/influx/InfluxClientProvider.java` | 엔드포인트별 공유 HTTP 클라이언트 |
| `service/influx/InfluxMetaCache.java` | 보관정책 · 존재 확인 · measurement 목록 |
| `service/influx/ChunkedMetricQuery.java` | 슬라이싱·병합 계산 (I/O 없음 - 그래서 단위 테스트 가능) |
| `service/influx/ChunkedMetricQueryService.java` | 슬라이스 실행과 슬라이스 캐시 |
| `model/influx/InfluxQl.java` | `buildQuery`(상대 구간) / `buildRangeQuery`(절대 구간) |
| `config/MonitoringCacheProperties.java` | `monitoring.cache.*` 바인딩 |
| `config/ChunkCacheProperties.java` | `monitoring.cache.chunk.*` 바인딩 |

### 캐시 키 서명

`MonitoringCacheKey.signatureOf` 가 만드는 문자열입니다. **이 문자열이 다르면 다른 캐시 칸입니다.**
예열이 채운 항목을 화면이 못 읽는 사고가 전부 여기서 납니다.

```
m=<measurement>|r=<range>|gt=<group_time>|lim=<limit>|gb=<group_by,정렬>|f=<함수:필드,정렬>|c=<조건,정렬>
```

- `gb` · `f` · `c` 는 값이 있을 때만 붙습니다. 비면 세그먼트 자체가 없습니다.
- `ns_id` · `infra_id` · `node_id` 조건은 키 튜플에 따로 들어가므로 `c` 에서 빠집니다.
- 순서는 정렬해서 없앱니다. 클라이언트가 필드를 다른 순서로 보내도 같은 칸입니다.

실제 값 예시입니다.

| 보내는 쪽 | 서명 |
|---|---|
| 화면 개요 · 대시보드 | `m=cpu\|r=1h\|gt=1m\|lim=2000\|gb=node_id\|f=mean:usage_idle` |
| insight 이상탐지 | `m=cpu\|r=12h\|gt=1m\|lim=\|f=mean:usage_idle` |
| insight 예측 | `m=cpu\|r=<범위>\|gt=1h\|lim=\|f=mean:usage_idle` |
| 예열 `overview` | 화면 개요와 동일 |
| 예열 `realtime` · `longrange` | `m=cpu\|r=12h\|gt=5m\|lim=` ← **어느 클라이언트와도 안 맞음** |

---

## 2. 설정 레퍼런스

전부 환경변수로 덮어쓸 수 있습니다. 기본값은 `src/main/resources/application.yaml` 입니다.

### 메트릭 결과 캐시 - `monitoring.cache.*`

| 환경변수 | 키 | 기본값 | 의미 |
|---|---|---|---|
| `MONITORING_CACHE_ENABLED` | `enabled` | `true` | 끄면 모든 조회가 InfluxDB 직행 |
| `MONITORING_CACHE_MIN_BUCKET_SECONDS` | `min-bucket-seconds` | `60` | **fresh window 하한.** `group_time` 이 없거나 못 읽으면 이 값 |
| `MONITORING_CACHE_MAX_BUCKET_SECONDS` | `max-bucket-seconds` | `3600` | fresh window 상한 |
| `MONITORING_CACHE_EMPTY_TTL_SECONDS` | `empty-ttl-seconds` | `60` | 빈 결과 전용 fresh window |
| `MONITORING_CACHE_HARD_TTL_SECONDS` | `hard-ttl-seconds` | `900` | 절대 수명. 넘으면 stale 도 폐기 |
| `MONITORING_CACHE_REFRESH_THREADS` | `refresh-threads` | `8` | 백그라운드 갱신 스레드 |
| `MONITORING_CACHE_MAX_WEIGHT_MB` | `max-weight-mb` | `512` | 캐시 총량 상한 (추정 바이트 기준) |
| `MONITORING_CACHE_BYTES_PER_POINT` | `estimated-bytes-per-point` | `200` | 데이터 점 하나의 추정 크기 |

> `min-bucket-seconds` / `max-bucket-seconds` 는 **시간 버킷이 아니라 fresh window 의 하한·상한**입니다.
> 이름이 예전 설계의 잔재입니다.

### 조회 이력 추적 - `monitoring.cache.access-tracker.*`

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| `MONITORING_CACHE_ACCESS_MAX_SIZE` | `2000` | 추적할 노드 수 상한 |
| `MONITORING_CACHE_ACCESS_WINDOW_SECONDS` | `900` | 이 시간 안의 조회만 집계 (최소 60) |

### 예열 - `monitoring.cache.warm.*`

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| `MONITORING_CACHE_WARM_ENABLED` | `true` | 전체 예열 on/off |
| `MONITORING_CACHE_WARM_TOP_N` | `10` | 한 패스에 데울 노드 수 |
| `MONITORING_CACHE_WARM_SELECTION` | `recently-queried` | 대상 선정. `recently-created` 로 두면 생성순 |
| `MONITORING_CACHE_WARM_TIMEBOX_SECONDS` | `45` | 한 패스 제한 시간 (최소 5) |
| `MONITORING_CACHE_WARM_JITTER_SECONDS` | `10` | 시작 시각을 흩뜨리는 랜덤 지연 상한 |
| `MONITORING_CACHE_WARM_REALTIME_CRON` / `_THREADS` | `0 * * * * *` / `10` | realtime 작업 |
| `MONITORING_CACHE_WARM_LONGRANGE_CRON` / `_THREADS` | `0 5 * * * *` / `10` | longrange 작업 |
| `MONITORING_CACHE_WARM_OVERVIEW_ENABLED` / `_CRON` / `_THREADS` | `true` / `0 * * * * *` / `10` | overview 작업 |

`ranges` 와 `overview.queries` 는 환경변수가 아니라 yaml 로만 바꿉니다.

```yaml
monitoring:
  cache:
    warm:
      realtime:
        ranges:
          - { range: 1h,  group-time: 1m }
          - { range: 6h,  group-time: 5m }
          - { range: 12h, group-time: 5m }
      longrange:
        ranges:
          - { range: 1d, group-time: 5m }
          - { range: 3d, group-time: 15m }
          - { range: 5d, group-time: 30m }
          - { range: 7d, group-time: 1h }
      overview:
        # 화면이 보내는 모양과 한 글자라도 다르면 그 항목은 안 읽힙니다 (1장 캐시 키 서명 참조)
        queries:
          - { measurement: cpu,  function: mean, field: usage_idle,   range: 1h, group-time: 1m, limit: 2000 }
          - { measurement: mem,  function: mean, field: used_percent, range: 1h, group-time: 1m, limit: 2000 }
          - { measurement: disk, function: mean, field: used_percent, range: 1h, group-time: 1m, limit: 2000 }
```

`realtime` · `longrange` 의 `ranges` 항목에는 `fields` 를 줄 수 없습니다.
`buildRequest`(`MonitoringCacheWarmScheduler.java:438`)가 항상 빈 `fields` 로 요청을 만들기 때문에
**이 두 작업은 구조적으로 집계 없는 `SELECT *` 만 데웁니다.**

### 보조 캐시 · 접속 - `influx.*`

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| `INFLUX_CLIENT_MAX_IDLE_CONNECTIONS` | `32` | 공유 클라이언트의 유휴 커넥션 수 |
| `INFLUX_CLIENT_KEEP_ALIVE_SECONDS` | `300` | 커넥션 유지 시간 |
| `INFLUX_CLIENT_CONNECT_TIMEOUT_SECONDS` | `5` | 접속 제한 시간 |
| `INFLUX_CLIENT_READ_TIMEOUT_SECONDS` | `30` | 읽기 제한 시간 |
| `INFLUX_META_RP_TTL_SECONDS` | `3600` | 보관정책 기억 시간 |
| `INFLUX_META_EXISTS_TTL_SECONDS` | `60` | "있다" 결과 기억 시간 |
| `INFLUX_META_EXISTS_NEGATIVE_TTL_SECONDS` | `15` | "없다" 결과 기억 시간 |
| `INFLUX_META_MEASUREMENT_TTL_SECONDS` | `600` | 노드별 measurement 목록 기억 시간 |

### 스케줄러

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| `SPRING_SCHEDULING_POOL_SIZE` | `4` | 주기 작업 스레드. **1이면 작업들이 서로 밀립니다** |

### 상황별 조절

| 상황 | 조절 |
|---|---|
| 화면이 너무 늦게 갱신됨 | `min-bucket-seconds` 를 낮춘다 (예: 30) |
| InfluxDB 부하가 높음 | `min-bucket-seconds` 를 올리거나 `top-n` 을 줄인다 |
| 힙이 부담됨 | `max-weight-mb` 를 줄인다. `estimated-bytes-per-point` 가 실제와 맞는지도 본다 |
| 예열이 자꾸 timebox 에 걸림 | `top-n` 을 줄이거나 `timebox-seconds` 를 늘린다 |
| 예열 부하만 줄이고 싶음 | `realtime` · `longrange` 의 cron 을 늦추거나 `ranges` 를 줄인다 (4장 참고) |
| 캐시를 끄고 싶음 | `monitoring.cache.enabled: false` |

---

## 3. 지표 읽는 법

```bash
curl -s http://<manager>:18080/api/o11y/monitoring/influxdb/cache/stats | jq .data
```

응답은 메트릭 캐시 통계에 `influxMeta`, `influxSharedClients`, `chunk`, `warm` 이 얹힌 구조입니다.

### 메트릭 캐시

| 필드 | 의미 | 정상 범위 / 조치 |
|---|---|---|
| `hitRate` | `(hitFresh+hitStale) / 전체` | 낮으면 예열 대상·fresh window 를 의심 |
| `hitFreshCount` | fresh 구간 내 응답 | 대부분 여기여야 정상 |
| `hitStaleCount` | 만료 후 즉시 반환 + 뒤에서 갱신 | 과도하면 예열이 못 따라가는 것 |
| `missCount` | 로드하며 기다린 요청 | 재기동 직후 말고는 적어야 함 |
| `servedAgeAvgMillis` / `servedAgeMaxMillis` | **실제로 내보낸 데이터의 나이** | `max` 가 `hard-ttl` 에 붙어 있으면 갱신이 실패 중 |
| `emptyCachedCount` | 빈 결과 저장 횟수 | 과도하면 없는 조합을 계속 물어보는 중 |
| `refreshSubmittedCount` | 백그라운드 갱신 제출 | |
| `refreshRejectedCount` | 갱신 제출 거부 | 스레드 풀 큐가 무제한이라 사실상 0 |
| `refreshFailedCount` | 갱신 중 예외 | 0이 아니면 InfluxDB 상태 확인 |
| `evictionCount` / `evictionWeight` | 용량 부족 축출 | 계속 늘면 `max-weight-mb` 상향 검토 |
| `estimatedSize` | 항목 수 | |
| `trackedVmCount` | 조회 이력이 잡힌 노드 수 | 0이면 예열이 생성순 폴백으로 도는 중 |

### `influxMeta` · `influxSharedClients`

| 필드 | 정상 |
|---|---|
| `retentionPolicyHits` / `Misses` | hits ≫ misses |
| `existsHits` / `Misses` | hits ≫ misses |
| `measurementEntries` | 예열이 도는 만큼 채워짐 |
| `influxSharedClients` | **등록된 InfluxDB 인스턴스 수와 같아야** 정상. 계속 늘면 클라이언트를 새로 만들고 있는 것 |

### `warm`

| 필드 | 의미 |
|---|---|
| `passesRun` | 실행된 패스 수 |
| `passesSkippedOverlap` | 이전 패스가 안 끝나 건너뜀. 계속 늘면 주기 안에 못 끝나는 것 |
| `passesTimedOut` | timebox 초과로 포기 |
| `selection` / `topN` | 현재 적용된 설정값 |

### 로그

```
[MON-CACHE] enabled minBucketSec=60, maxBucketSec=3600, emptyTtlSec=60, hardTtlSec=900, ...
[CACHE-WARM] initialized selection=recently-queried, topN=10, timeboxSec=45, jitterSec=10, ...
[CACHE-WARM:realtime] vms=10, ranges=3, ok=42, skipped=78, fail=0, took=1832ms
[CACHE-WARM:overview] vms=10, queries=3, ok=30, fail=0, took=410ms
[INFLUX-META] enabled rpTtlSec=3600, existsTtlSec=60, existsNegTtlSec=15, measurementTtlSec=600
[INFLUX-CLIENT] creating shared client for url=..., user=...
```

`[CACHE-WARM:*]` 의 `skipped` 는 아직 fresh 라서 다시 안 읽은 건수입니다. 정상 동작입니다.

### 수동 조작

```bash
# 메트릭 캐시 + 슬라이스 캐시 비우기
curl -s -X DELETE http://<manager>:18080/api/o11y/monitoring/influxdb/cache

# 즉시 예열 (세 작업을 순차로 돌리고 완료까지 대기)
curl -s -X POST http://<manager>:18080/api/o11y/monitoring/influxdb/cache/warm
```

---

## 4. 청크 캐싱 (기본 off)

시계열의 과거 구간은 변하지 않으므로, 조회 구간을 절대 시각 슬라이스로 잘라 두면 다음 조회에서는
가장자리만 읽으면 됩니다.

```
  ├──partial──┼─ 재사용 ─┼─ 재사용 ─┼─ 재사용 ─┼─ 재사용 ─┼─partial─┤
   왼쪽 가장자리                                          오른쪽 가장자리
   (창이 밀려서 잘림)                                  (아직 채워지는 중)
       ↑ 이 둘만 실제로 조회
```

### 자르는 절차

1. **창을 집계 간격에 맞춰 정렬한다.** `alignedEnd = floor(now / step) * step`, `start = alignedEnd - range`.
   "지금" 이 아니라 마지막으로 완성된 버킷에서 끝냅니다.
2. **슬라이스 폭은 step 의 배수.** 집계 버킷이 슬라이스 경계를 걸치지 않게 합니다.
   `range / targetChunks` 를 step 단위로 내림하고, 슬라이스 수가 `maxChunks` 를 넘으면 폭을 늘립니다.
3. **완성되고 안정된 슬라이스만 캐싱한다.** 슬라이스가 닫힌 뒤 `settle`(기본 = step 하나)만큼 더
   지나야 저장합니다. 늦게 도착하는 쓰기가 최근 버킷에 들어오기 때문입니다.
4. **병합.** 슬라이스는 서로 겹치지 않고 각각 최신순이므로, 최신 슬라이스부터 이어 붙이면
   타임스탬프를 파싱하지 않고도 전체가 최신순이 됩니다. 계열은 `(name, tags)` 로 구분합니다.

### 안 자르는 경우 (단일 쿼리로 폴백)

| 조건 | 이유 |
|---|---|
| 집계 함수가 없음 | `GROUP BY time()` 이 안 붙어 점이 임의 시각에 놓임 |
| `group_time` / `range` 를 못 읽음 | 정렬 기준이 없음 |
| `range <= group_time` | 자를 게 없음 |
| `limit` 이 실제로 잘라낼 수 있음 (`limit < range/step`) | InfluxDB 가 쿼리마다 limit 을 적용하므로 슬라이스별로 잘림 |
| `range < min-range-seconds` (기본 900초) | 잘라도 이득이 없음 |
| 슬라이스가 2개 미만 | 위와 같음 |

계획 중이든 실행 중이든 예외가 나면 단일 쿼리로 폴백합니다(`fallbacks` 카운터).

### 알아둘 차이

**청크 경로와 단일 쿼리는 읽는 구간이 정확히 같지 않습니다.**

```
단일 쿼리 : where time > now() - range        →  [now - range,        now       )
청크 합계 :                                      [alignedEnd - range, alignedEnd)
```

`alignedEnd` 는 `now` 를 step 경계로 내린 값이므로 두 구간은 최대 `step` 만큼 어긋나고,
**아직 채워지는 중인 마지막 버킷이 청크 결과에서 빠집니다.** 켜기 전에 화면 쪽에서 이 차이가
문제가 되지 않는지 확인하세요.

### 설정과 지표

```yaml
monitoring:
  cache:
    chunk:
      enabled: false          # 기본 off
      target-chunks: 12       # 구간을 대략 몇 조각으로
      max-chunks: 48          # 조각 수 상한
      min-range-seconds: 900  # 이보다 짧은 구간은 안 자름
      settle-seconds: 0       # 0 = 집계 간격 하나만큼 기다렸다 캐싱
      max-slices: 20000       # 슬라이스 캐시 개수 상한 (용량 상한은 없음)
      slice-ttl-seconds: 1800
```

```bash
curl -s .../cache/stats | jq .data.chunk
```

| 필드 | 의미 |
|---|---|
| `plansChunked` / `plansRejected` | 실제로 잘린 요청 / 조건이 안 맞아 단일 쿼리로 간 요청 |
| `sliceHits` / `sliceMisses` | 슬라이스 재사용 / 신규 조회 |
| `sliceUncacheable` | 가장자리라 캐싱 대상이 아닌 슬라이스 |
| `fallbacks` | 오류로 단일 쿼리로 되돌아간 횟수. **0이 아니면 원인 확인** |
| `sliceEntries` | 슬라이스 캐시 항목 수 |

`sliceHits` 가 `sliceMisses` 보다 뚜렷하게 크지 않으면 이 환경에서는 켤 이유가 없습니다.

### 왜 기본 off 인가

사용자 응답 지연은 이미 결과 캐시와 SWR 로 해결돼 있습니다. 청크 캐싱이 줄이는 것은
**InfluxDB 가 훑는 데이터 양**이고, 그게 문제인지는 환경마다 다릅니다.
켜는 판단은 InfluxDB 부하를 실제로 측정한 뒤에 하세요.

---

## 5. 테스트

| 테스트 | 개수 | 무엇을 |
|---|---|---|
| `service/cache/MonitoringCacheServiceTest` | 7 | 재조회 적중, fresh window = `group_time`, 동시 miss single-flight, 빈 결과 캐싱, `refreshNow` 강제 갱신, 반환 리스트 불변, stale 즉시 반환 + 백그라운드 갱신 |
| `service/cache/ChunkedMetricQueryTest` | 11 | 슬라이스가 구간을 빈틈·중복 없이 덮는지, 경계가 step 배수인지, 최신·부분 슬라이스가 캐싱 제외되는지, 거절 조건 4종, 병합 순서·계열 분리·limit |

**주의: 지금 `./gradlew test` 로는 이 테스트들이 실행되지 않습니다.**
같은 테스트 소스셋의 아래 세 파일이 컴파일되지 않아 `compileTestJava` 에서 빌드가 멈춥니다.
2026-06-09 의 MCI/VM → Infra/Node 리네임 이후 계속된 상태이며 캐시 코드와는 무관합니다.

| 파일 | 원인 |
|---|---|
| `service/VMServiceImplTest.java` | `findByNsIdAndMciIdAndVmId`, `getVmId` 가 없어짐 |
| `controller/OtelJavaControllerTest.java` | `ResultDTOBuilder.mciId` 가 없어짐 |
| `controller/BeylaControllerTest.java` | 위와 동일 |

세 파일을 제외하면 캐시 테스트 18개는 전부 통과합니다.

---

## 6. 부록 - 무엇이 어떻게 바뀌었나

캐시가 지금 형태가 되기 전의 동작입니다. 예전 코드를 아는 사람과 이야기할 때만 필요합니다.

| 항목 | 이전 | 현재 |
|---|---|---|
| 캐시 키 | 벽시계를 1시간 칸으로 양자화해 키에 포함 | 키에 시각 없음. 수명을 항목에 둠 |
| 신선도 | 쿼리와 무관하게 1시간 고정 → 최대 59분 묵은 값 | 쿼리의 `group_time` |
| 만료 | 칸이 바뀌는 정각에 **모든 키가 동시에 miss** | 항목마다 만료 시점이 흩어짐 |
| 만료 후 | 버리고 다시 읽음 (사용자가 기다림) | 즉시 반환 + 백그라운드 갱신 |
| 동시 miss | 요청 수만큼 InfluxDB 조회 | 키당 1회 (single-flight) |
| 빈 결과 | 적중으로 안 쳐서 매 요청마다 재조회 | 60초 캐싱 |
| 저장 대상 | **생성 7일 이내 노드만** 캐싱 → 운영 환경에서 캐시가 사실상 무동작 | 생성 시각으로 가르지 않음 |
| 축출 | - | 용량 기반 (512MB) |
| miss 1건당 왕복 | 3~4회 (보관정책 · 존재 확인 · 다운샘플 확인 · 데이터) | 1회 (앞 셋은 메모이즈) |
| HTTP 클라이언트 | 쿼리마다 생성 후 폐기 | 엔드포인트당 1개 공유 |
| 예열 경로 | 일반 조회 경로 → 첫 틱 이후 무동작 | 강제 갱신 경로 |
| 예열 대상 | 생성 시각 내림차순 | 최근 15분 조회 횟수 순 (없으면 생성순 폴백) |
| 예열 범위 | 전체 measurement × 전체 노드 (대부분 빈 결과) | 노드가 실제로 보내는 measurement 만 |
| 예열 실행 | 스케줄러 스레드에서 완료까지 대기 → 작업끼리 밀림 | 분리 실행, timebox 45초, jitter, 중복 스킵 |
| 스케줄러 스레드 | 1 (스프링 기본) | 4 |

관련 커밋입니다.

| 커밋 | 내용 |
|---|---|
| `acd3071d6` | 캐시 키·수명 재설계, SWR, single-flight, 빈 결과 캐싱, 예열 재작성, 보조 캐시·공유 클라이언트 도입 |
| `2cf50fc06` | 문서화 |
| `4725087b0` | 청크 캐싱 추가 (기본 off) |
