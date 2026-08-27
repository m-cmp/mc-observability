# 모니터링 메트릭 캐시

o11y-manager가 InfluxDB에서 읽는 메트릭 조회 결과를 메모리에 캐싱하는 계층에 대한 설명입니다.
왜 이렇게 만들었는지, 어떻게 동작하는지, 무엇을 보고 판단하면 되는지를 정리했습니다.

> 캐시 전반(모니터링 캐시 + CSP 캐시 + 보조 캐시)을 쉽게 훑어보려면
> [`docs/info/cache_architecture.md`](../../../docs/info/cache_architecture.md)를 먼저 보세요.

## 전체 그림

```
   화면 / 리포팅 / 자동 분석
            │  (자원, 지표, range, group_time)
            ▼
 ┌─────────────────────────────────────────────────────────────────┐
 │ MonitoringCacheService                                          │
 │                                                                 │
 │   신선함  ──────────────────────────────────► 즉시 응답          │
 │   만료됨  ──► 즉시 응답 + 백그라운드 갱신 요청 ──┐                │
 │   없음    ──► single-flight 로드 ──────────────┤                │
 │                                               │                │
 │   QueryAccessTracker (누가 조회했나) ──► 워머 대상 선정           │
 └───────────────────────────────────────────────┼─────────────────┘
                                                 │
                        ┌────────────────────────▼──────────────────┐
                        │ InfluxDbServiceImpl.loadMetricsBy…        │
                        │                                           │
                        │  InfluxMetaCache                          │
                        │    · 보관정책        (1시간 기억)          │
                        │    · 존재 확인       (60초 / 없으면 15초)   │
                        │    · VM별 측정항목    (10분 기억)          │
                        │        → 예전엔 이 셋이 매번 왕복이었음      │
                        │                                           │
                        │  InfluxClientProvider                     │
                        │    · 엔드포인트당 접속 1개를 계속 재사용     │
                        │        → 예전엔 쿼리마다 새로 만들고 버림    │
                        └────────────────────────┬──────────────────┘
                                                 ▼
                                             InfluxDB
                                       (원본 / 다운샘플)

 ┌─ MonitoringCacheWarmScheduler ────────────────────────────────┐
 │  realtime(매분) · longrange(매시 5분) · overview(매분)          │
 │  → refreshMetricsByVM 으로 캐시를 강제로 덮어씀                  │
 │  → 스케줄러 스레드와 분리, 타임박스 45초, 지터 0~10초            │
 └───────────────────────────────────────────────────────────────┘
```

---

## 1. 이 캐시가 푸는 문제

대시보드는 같은 질문을 아주 자주 반복합니다. "이 VM의 최근 1시간 CPU를 1분 간격으로" 같은
요청이 화면을 열 때마다, 그리고 여러 사람이 동시에 보면 사람 수만큼 InfluxDB로 나갑니다.

그런데 이런 조회에는 캐싱을 어렵게 만드는 성질이 하나 있습니다. **조회 구간이 상대 시간**이라는
점입니다. `range=1h`는 "지금부터 1시간 전까지"이므로 1초만 지나도 이론적으로는 다른 질문입니다.
그렇다고 매번 새로 조회하면 캐시가 의미가 없습니다.

핵심은 **얼마나 묵은 데이터까지 같은 답으로 봐도 되는가**이고, 그 답은 쿼리 자신이 들고 있습니다.
`group_time=1m`으로 1분 단위 집계를 요청했다면, **새로운 데이터 포인트는 1분에 한 번 생깁니다.**
즉 1분 안에는 몇 번을 물어도 실질적으로 같은 답입니다.

이 캐시는 그 성질을 그대로 규칙으로 씁니다.

---

## 2. 세 가지 동작 원칙

### 2-1. 신선도는 쿼리의 집계 간격을 따른다

캐시 항목의 수명(fresh window)은 그 쿼리의 `group_time`입니다.

| 쿼리 | fresh window | 의미 |
|---|---|---|
| `range=1h, group_time=1m` | 60초 | 1분 넘게 묵은 답은 다시 만든다 |
| `range=7d, group_time=1h` | 3600초 | 1시간 단위 집계이므로 1시간까지는 같은 답 |
| `group_time` 없음 | 60초 (하한) | 판단 근거가 없으면 짧게 잡는다 |

하한/상한은 `min-bucket-seconds`(기본 60) / `max-bucket-seconds`(기본 3600)로 조절합니다.

```
[이전] 벽시계를 1시간 칸으로 양자화해 키에 포함

  0분        10분      20분      30분      40분      50분     59분
  ●───────────────────────────────────────────────────────────────►
  캐시 채움   이 칸이 끝날 때까지 계속 0분 시점 데이터를 반환
                                                   └─ 최대 59분 stale
  그리고 정각에 칸이 바뀌는 순간 → 모든 키가 동시에 미스 (절벽)

[현재] 키에 시각 없음 · 항목 수명 = group_time

  0분   1분   2분   3분   4분   5분  ...
  ●──┐  ●──┐  ●──┐  ●──┐  ●──┐  ●
     갱신   갱신   갱신   갱신   갱신
  └─ stale 상한 = 1분,  만료 시점이 항목마다 흩어져 절벽 없음
```

> **이전 동작과의 차이**
> 예전에는 수명이 쿼리와 무관하게 **1시간 고정**이었습니다. 그래서 1분 간격 차트를 보고 있어도
> 최대 59분 전에 만들어진 데이터를 받을 수 있었습니다. 게다가 정각마다 키가 한꺼번에 바뀌어
> 모든 조회가 동시에 미스가 됐습니다.

### 2-2. 만료되어도 기다리게 하지 않는다 (stale-while-revalidate)

fresh window가 지난 항목을 **버리지 않습니다.**

```
                          요청 도착
                             │
                             ▼
                   ┌──────────────────┐
                   │ 캐시에 있나?      │
                   └────┬────────┬────┘
                     있음         없음
                        │           │
                        ▼           │
              ┌──────────────────┐  │
              │ 아직 신선한가?     │  │
              └───┬─────────┬────┘  │
                 예        아니오    │
                  │          │      │
                  ▼          ▼      ▼
             ┌────────┐ ┌─────────────────┐ ┌──────────────────┐
             │ 즉시    │ │ 즉시 반환 +      │ │ 지금 로드 후      │
             │ 반환    │ │ 백그라운드 갱신   │ │ 반환·저장         │
             └────────┘ └─────────────────┘ └──────────────────┘
              hitFresh      hitStale             missCold
```

시간축으로 보면 이렇습니다.

```
  로드                fresh window 끝                 hard TTL
   ●───────────────────────┼──────────────────────────────┤
   │◄──── 그냥 반환 ──────►│◄─ 반환하며 뒤에서 갱신 ──────►│ 이후엔 폐기
        (hitFresh)              (hitStale)                  (missCold)
```

덕분에 **"수명이 끝났다"는 이유만으로 사용자가 InfluxDB를 기다리는 일이 없습니다.**
백그라운드 갱신은 전용 스레드 풀(`refresh-threads`, 기본 8)에서 처리하고, 같은 항목에 대해서는
동시에 하나만 돕니다.

계속 갱신이 실패하면 무한정 옛날 데이터를 줄 수는 없으므로, `hard-ttl-seconds`(기본 900초)가
지나면 항목을 아예 버리고 다음 요청은 정상적으로 다시 읽습니다.

### 2-3. 같은 키는 동시에 한 번만 읽는다 (single-flight)

같은 쿼리로 요청 20개가 동시에 들어와도 InfluxDB로는 **한 번만** 나갑니다.
나머지 19개는 그 결과를 같이 받습니다.

```
[이전]                              [현재]

요청 1 ──────► InfluxDB             요청 1 ──┐
요청 2 ──────► InfluxDB             요청 2 ──┤
요청 3 ──────► InfluxDB             요청 3 ──┼──► InfluxDB (1회)
   ⋮                                   ⋮     │
요청 20 ─────► InfluxDB             요청 20 ─┘
   20회 조회                           1회 조회
```

이게 없으면 캐시가 비어 있는 순간(재기동 직후, 신규 VM 등)에 요청이 몰리면서
InfluxDB를 동시에 두들기게 됩니다.

---

## 3. 빈 결과도 캐싱합니다

GPU가 없는 VM에 `dcgm` 측정값을 물어보면 정상적으로 **빈 결과**가 나옵니다.
예전에는 이런 조합이 캐시 적중으로 인정되지 않아서 **요청이 올 때마다 매번 InfluxDB로 나갔습니다.**

지금은 빈 결과도 `empty-ttl-seconds`(기본 60초) 동안 캐싱합니다.

- 없는 데이터를 반복 조회하지 않습니다.
- 60초짜리 짧은 수명이라, 이제 막 데이터를 보내기 시작한 VM도 금방 화면에 나타납니다.

---

## 4. 조회 1회에 InfluxDB를 몇 번 다녀오나

예전에는 캐시 미스 1건마다 InfluxDB를 **3~4번** 다녀왔습니다.

| 단계 | 목적 | 지금 |
|---|---|---|
| `SHOW RETENTION POLICIES` | 기본 retention policy 확인 | **1시간 캐싱** (거의 안 바뀜) |
| `SHOW TAG VALUES` | 해당 ns/mci/vm이 존재하는지 확인 | **60초 캐싱** (없을 때는 15초) |
| 다운샘플 DB 존재 확인 | 장기 조회 라우팅 판단 | 위와 동일하게 캐싱 |
| 실제 데이터 쿼리 | — | 그대로 |

```
[이전] 미스 1건마다

  요청 ──► ① 보관정책 조회        ──► InfluxDB   (새 클라이언트 생성 후 폐기)
       ──► ② 자원 존재 확인       ──► InfluxDB   (새 클라이언트 생성 후 폐기)
       ──► ③ 다운샘플 DB 존재 확인 ──► InfluxDB   (새 클라이언트 생성 후 폐기)
       ──► ④ 실제 데이터 쿼리      ──► InfluxDB   (새 클라이언트 생성 후 폐기)

[현재]

  요청 ──► ①②③ 메모이즈된 값 사용 (왕복 없음)
       ──► ④ 실제 데이터 쿼리 ──► InfluxDB   (엔드포인트당 공유 클라이언트 재사용)
```

**결과적으로 미스 1건당 왕복이 3~4회에서 1회로 줄었습니다.**

여기에 더해, 예전에는 **쿼리 한 번마다 HTTP 클라이언트를 새로 만들고 버렸습니다.**
매번 커넥션 풀·소켓·TLS 핸드셰이크를 새로 하는 셈이라 순수한 낭비였습니다.
지금은 엔드포인트(URL+계정)별로 **하나의 클라이언트를 공유**하며 커넥션을 재사용합니다.

---

## 5. 사전 예열(warming)

사용자가 조회하기 전에 미리 캐시를 채워두는 백그라운드 작업입니다. 세 종류가 돕니다.

| 작업 | 주기 | 대상 구간 |
|---|---|---|
| `realtime` | 매분 | 1h/1m, 6h/5m, 12h/5m |
| `longrange` | 매시 5분 | 1d/5m, 3d/15m, 5d/30m, 7d/1h |
| `overview` | 매분 | NS/MCI 개요 화면이 실제로 보내는 쿼리 모양 그대로 |

### 5-1. 예열은 캐시를 "덮어씁니다"

예열 작업은 일반 조회 경로가 아니라 **강제 갱신 경로**(`refreshMetricsByVM`)를 씁니다.

```
[이전] 예열이 일반 조회 경로를 호출

  1분: 워머 ─► getOrLoad ─► 캐시 적중 ─► 반환하고 끝     ← InfluxDB에 안 감
  2분: 워머 ─► getOrLoad ─► 캐시 적중 ─► 반환하고 끝     ← 갱신도 안 함
   ⋮
 59분: 데이터는 여전히 첫 틱 시점 값

[현재] 예열이 강제 갱신 경로를 호출

  1분: 워머 ─► refreshMetricsByVM ─► InfluxDB ─► 덮어씀  ●
  2분: 워머 ─► refreshMetricsByVM ─► InfluxDB ─► 덮어씀  ●
   ⋮  (단, 아직 신선한 항목은 isMetricCacheFresh 로 건너뜀 → skipped)
```

> **이전 동작과의 차이**
> 예전 예열 작업은 일반 조회 경로를 호출했습니다. 그래서 캐시에 값이 있으면 그대로 반환하고
> 끝났습니다. 즉 **첫 틱에 한 번 채운 뒤로는 아무 일도 하지 않았고**, 데이터는 그 시점에
> 멈춰 있었습니다. 매분 도는 작업이 사실상 무동작이었던 셈입니다.

단, 아직 신선한 항목은 다시 읽지 않고 건너뜁니다(로그의 `skipped`).

### 5-2. 예열 대상은 "실제로 보는 VM"

기본값은 `recently-queried`입니다. 최근 15분간 **실제로 조회된 VM**을 조회 횟수 순으로 고릅니다.
트래픽이 아직 없으면(재기동 직후 등) 최근 생성된 VM으로 채웁니다.

`selection: recently-created`로 두면 예전처럼 생성 시각 순으로만 고릅니다.

```
[이전]  생성 시각 내림차순 top-N
        └─ 실제로 보고 있는 VM이 몇 달 전 생성분이면 예열이 빗나감

[현재]  최근 15분 조회 이력 기준 top-N
        └─ 트래픽이 없으면(재기동 직후) 생성 시각 순으로 보충
```

> **왜 바꿨나**
> 생성 시각은 수요를 나타내지 못합니다. 운영자가 지금 열어놓고 보는 VM은 몇 달 전에 만든
> VM일 수도 있습니다.

### 5-3. VM이 실제로 보내는 항목만 예열

예열 전에 해당 VM이 실제로 어떤 measurement를 보내는지 조회해서(10분 캐싱) 그 조합만 채웁니다.

```
[이전]  VM 10개 × 전체 measurement 12개 × range 3개 = 360 쿼리
        대부분이 빈 결과 (GPU 없는 VM에 dcgm, Windows VM에 리눅스 항목 …)

[현재]  VM별 실제 measurement 목록을 먼저 조회(10분 메모이즈)
        존재하는 조합만 쿼리
```

> **이전 동작과의 차이**
> 예전에는 **전체 measurement × 전체 대상 VM**을 전부 돌았습니다. GPU 없는 VM에 `dcgm`을,
> Windows VM에 리눅스 전용 항목을 조회하는 식이라 대부분이 빈 결과였습니다.

### 5-4. 스케줄러를 막지 않습니다

- 예열 패스는 스케줄러 스레드에서 **분리되어** 실행됩니다.
- `timebox-seconds`(기본 45초)를 넘으면 남은 작업을 포기합니다.
- `jitter-seconds`(기본 10초) 범위의 랜덤 지연을 줘서 여러 작업이 정각에 동시에 몰리지 않게 합니다.
- 이전 패스가 아직 돌고 있으면 이번 틱은 건너뜁니다(`passesSkippedOverlap`).

> **이전 동작과의 차이**
> Spring 스케줄러는 기본이 스레드 1개인데, 매분 0초에 여러 작업이 동시에 트리거되고
> 각자 완료를 기다렸습니다. 하나가 밀리면 나머지가 줄줄이 밀렸습니다.
> 지금은 `spring.task.scheduling.pool.size`를 4로 두고, 대기도 하지 않습니다.

---

## 6. 오래된 VM도 캐싱합니다

> **이전 동작과의 차이 — 가장 영향이 컸던 부분**
> 예전에는 캐시 수명을 "VM 생성 시각 + 7일"로 계산했고, **생성한 지 7일이 지난 VM은 아예
> 캐시에 넣지 않았습니다.** 프로덕션 VM은 대부분 7일보다 오래됐으므로, 실제 운영 환경에서는
> 캐시가 거의 동작하지 않았습니다.

지금은 생성 시각으로 캐싱 여부를 가르지 않습니다. 어떤 항목을 남기고 버릴지는 Caffeine의
용량 기반 축출(`max-weight-mb`, 기본 512MB)에 맡깁니다. 자주·최근 조회된 항목이 자연히 남습니다.

---

## 7. 설정

`application.yaml` 기준입니다. 모두 환경변수로 덮어쓸 수 있습니다.

```yaml
monitoring:
  cache:
    enabled: true
    min-bucket-seconds: 60        # fresh window 하한
    max-bucket-seconds: 3600      # fresh window 상한
    empty-ttl-seconds: 60         # 빈 결과 캐싱 시간
    hard-ttl-seconds: 900         # 이 시간이 지나면 stale 항목도 폐기
    refresh-threads: 8            # 백그라운드 갱신 스레드
    max-weight-mb: 512            # 캐시 총량 상한
    access-tracker:
      max-size: 2000              # 조회 이력 추적 대상 수
      window-seconds: 900         # 조회 이력 유지 기간
    warm:
      enabled: true
      top-n: 10                   # 한 패스에 예열할 VM 수
      selection: recently-queried # 또는 recently-created
      timebox-seconds: 45
      jitter-seconds: 10

influx:
  client:                         # 엔드포인트별 공유 HTTP 클라이언트
    max-idle-connections: 32
    keep-alive-seconds: 300
    connect-timeout-seconds: 5
    read-timeout-seconds: 30
  meta:                           # 보조 조회 캐싱
    retention-policy-ttl-seconds: 3600
    exists-ttl-seconds: 60
    exists-negative-ttl-seconds: 15
    measurement-ttl-seconds: 600
```

### 상황별 조절 가이드

| 상황 | 조절할 값 |
|---|---|
| 화면이 너무 느리게 갱신됨 | `min-bucket-seconds`를 낮춘다 (예: 30) |
| InfluxDB 부하가 높음 | `min-bucket-seconds`를 올리거나 `top-n`을 줄인다 |
| 메모리 사용량이 부담됨 | `max-weight-mb`를 줄인다 |
| 예열이 자꾸 timebox에 걸림 | `top-n`을 줄이거나 `timebox-seconds`를 늘린다 |
| 캐시를 끄고 싶음 | `monitoring.cache.enabled: false` |

---

## 8. 운영 중 확인 방법

### 조회

```bash
curl -s http://<manager>:18080/api/o11y/monitoring/influxdb/cache/stats | jq
```

### 봐야 할 값

| 필드 | 의미 | 판단 기준 |
|---|---|---|
| `hitRate` | 캐시 적중률 | 낮으면 예열 대상이나 fresh window를 의심 |
| `hitFreshCount` / `hitStaleCount` | 신선한 응답 / 묵은 응답 비율 | stale이 과도하면 예열이 못 따라가는 것 |
| `servedAgeAvgMillis` / `servedAgeMaxMillis` | 실제로 내보낸 데이터가 얼마나 묵었는지 | **화면 신선도 문제를 여기서 잡습니다** |
| `emptyCachedCount` | 빈 결과 캐싱 횟수 | 과도하면 예열 대상 선정을 점검 |
| `refreshFailedCount` | 백그라운드 갱신 실패 | 0이 아니면 InfluxDB 상태 확인 |
| `evictionCount` | 용량 부족 축출 | 계속 늘면 `max-weight-mb` 상향 검토 |
| `influxMeta.*` | 보조 조회 캐시 적중 | hits가 misses보다 훨씬 커야 정상 |
| `influxSharedClients` | 공유 클라이언트 수 | InfluxDB 인스턴스 수와 같아야 정상 |
| `warm.passesSkippedOverlap` | 예열 패스 중복 스킵 | 계속 늘면 예열이 주기 안에 못 끝나는 것 |
| `warm.passesTimedOut` | timebox 초과 | 위와 동일 |
| `trackedVmCount` | 조회 이력 추적 중인 VM 수 | 0이면 예열이 생성순 폴백으로 동작 중 |

### 수동 조작

```bash
# 캐시 전체 비우기
curl -s -X DELETE http://<manager>:18080/api/o11y/monitoring/influxdb/cache

# 즉시 예열 (완료까지 대기)
curl -s -X POST http://<manager>:18080/api/o11y/monitoring/influxdb/cache/warm
```

### 로그

```
[MON-CACHE] enabled minBucketSec=60, maxBucketSec=3600, emptyTtlSec=60, hardTtlSec=900, ...
[CACHE-WARM] initialized selection=recently-queried, topN=10, timeboxSec=45, jitterSec=10, ...
[CACHE-WARM:realtime] vms=10, ranges=3, ok=42, skipped=78, fail=0, took=1832ms
[INFLUX-CLIENT] creating shared client for url=http://mc-observability-influx:8086, user=...
```

---

## 9. 코드 위치

| 파일 | 역할 |
|---|---|
| `service/cache/MonitoringCacheService.java` | 캐시 본체 — SWR, single-flight, 빈 결과 처리, 통계 |
| `service/cache/MonitoringCacheKey.java` | 캐시 키와 fresh window 산정 |
| `service/cache/MonitoringCacheWarmScheduler.java` | 예열 작업 |
| `service/cache/QueryAccessTracker.java` | 조회 이력 추적 (예열 대상 선정용) |
| `service/influx/InfluxClientProvider.java` | 엔드포인트별 공유 HTTP 클라이언트 |
| `service/influx/InfluxMetaCache.java` | retention policy · 존재 확인 · measurement 목록 캐싱 |
| `config/MonitoringCacheProperties.java` | 설정 바인딩 |

---

## 10. 청크 캐싱 (기본 off)

시계열의 **과거 구간은 변하지 않습니다.** "최근 1시간"을 조각으로 나눠 캐싱하면, 1분이 지났을 때
새로 읽어야 할 부분은 가장자리뿐입니다.

```
[기본 동작] 1분 지날 때마다 1시간을 통째로 다시 읽음

  ├──────────────────── 1시간 전부 재조회 ────────────────────┤


[청크 캐싱] 절대 시각 슬라이스로 잘라, 안 변하는 구간은 재사용

  ├──partial──┼─ 재사용 ─┼─ 재사용 ─┼─ 재사용 ─┼─ 재사용 ─┼─partial─┤
   왼쪽 가장자리                                            오른쪽 가장자리
   (창이 밀려서 잘림)                                   (아직 채워지는 중)
       ↑ 이 둘만 실제로 조회
```

### 어떻게 자르나

1. **창을 집계 간격에 맞춰 정렬** — `alignedEnd = floor(now / step) * step`, `start = alignedEnd - range`.
   "지금"이 아니라 **마지막으로 완성된 버킷**에서 끝냅니다.
2. **슬라이스 경계는 step의 배수** — 그래야 집계 버킷이 슬라이스 경계를 걸치지 않습니다.
3. **완성되고 안정된 슬라이스만 캐싱** — 슬라이스가 닫힌 뒤 `settle`(기본 = step 1개)만큼 더 지나야
   저장합니다. 늦게 도착하는 쓰기가 최근 버킷에 들어오기 때문입니다.
4. **병합** — 슬라이스는 서로 겹치지 않고 각각 최신순이므로, **최신 슬라이스부터 이어 붙이면**
   타임스탬프를 파싱하지 않고도 전체가 최신순이 됩니다.

### 안 자르는 경우 (그대로 단일 쿼리)

결과가 **완전히 동일하다고 증명되지 않으면 자르지 않습니다.**

| 조건 | 이유 |
|---|---|
| 집계 함수가 없음 | `GROUP BY time()`이 안 붙어 점이 임의 시각에 놓임 |
| `group_time` / `range`를 못 읽음 | 정렬 기준이 없음 |
| `limit`이 실제로 잘라낼 수 있음 | InfluxDB가 쿼리마다 limit을 적용하므로 슬라이스별로 잘림 |
| 구간이 너무 짧음 (기본 15분 미만) | 잘라도 이득이 없음 |
| 슬라이스가 상한을 넘음 | 과도한 팬아웃 방지 |

실행 중 예기치 못한 오류가 나도 **단일 쿼리로 폴백**합니다. 그래프가 깨지는 것보다 낫습니다.

### 왜 기본 off 인가

**사용자 응답 지연은 이미 해결돼 있습니다.** 결과 캐시와 SWR 덕분에 사용자는 InfluxDB를 기다리지
않습니다(측정값: 캐시 적중 14~19ms, 만료 항목도 28ms에 즉시 반환).

청크 캐싱이 줄이는 것은 **백그라운드에서 InfluxDB가 훑는 데이터 양**입니다. 그러니 켜는 판단은
"InfluxDB 부하가 실제로 문제인가"에 달려 있고, 그건 환경마다 다릅니다. 켠 뒤에는 아래 지표로
효과를 확인하세요.

```bash
curl -s .../cache/stats | jq .data.chunk
```

| 필드 | 의미 |
|---|---|
| `plansChunked` / `plansRejected` | 실제로 잘린 요청 / 조건이 안 맞아 단일 쿼리로 간 요청 |
| `sliceHits` / `sliceMisses` | 슬라이스 재사용 / 신규 조회 |
| `sliceUncacheable` | 가장자리라 캐싱 대상이 아닌 슬라이스 |
| `fallbacks` | 오류로 단일 쿼리로 되돌아간 횟수. **0이 아니면 원인 확인** |

`sliceHits`가 `sliceMisses`보다 뚜렷하게 크지 않다면 이 환경에서는 켤 이유가 없습니다.

### 설정

```yaml
monitoring:
  cache:
    chunk:
      enabled: false          # 기본 off
      target-chunks: 12       # 구간을 대략 몇 조각으로
      max-chunks: 48          # 조각 수 상한
      min-range-seconds: 900  # 이보다 짧은 구간은 안 자름
      settle-seconds: 0       # 0 = 집계 간격 1개만큼 기다렸다 캐싱
      max-slices: 20000
      slice-ttl-seconds: 1800
```

### 코드

| 파일 | 역할 |
|---|---|
| `service/influx/ChunkedMetricQuery.java` | 자르기·병합 계산 (I/O 없음 — 그래서 단위 테스트 가능) |
| `service/influx/ChunkedMetricQueryService.java` | 슬라이스 실행과 캐싱 |
| `config/ChunkCacheProperties.java` | 설정 |
| `model/influx/InfluxQl#buildRangeQuery` | 절대 구간 쿼리 생성 |
