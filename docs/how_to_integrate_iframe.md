# iframe 연동 가이드 (How to integrate via iframe)

mc-observability 프론트(`mc-observability-front`, 기본 포트 `18081`)를 다른 콘솔/포털에 **iframe으로 임베드**하는 방법을 설명합니다.

임베드 방식은 두 가지입니다.

- **한 덩어리로 임베드**: 네임스페이스 기반 URL 하나만 넘기면 프론트 상단 메뉴로 모든 기능(Monitoring / Logs / Config / Insight / Alerts / Tracing)을 제어합니다.
- **기능별로 나눠서 임베드**: 포털 쪽에 기능별 하위 메뉴를 두고 각 메뉴가 자기 기능만 담긴 iframe을 띄웁니다. 이때 프론트 상단 메뉴는 나오지 않습니다. → [5. 포털 하위 메뉴로 나눠서 임베드하기](#5-포털-하위-메뉴로-나눠서-임베드하기)

---

## 1. 한눈에 보기 — URL 패턴

| URL | 화면 | 용도 |
|---|---|---|
| `/` | 네임스페이스 선택 화면 | ns를 모를 때(미전달 시) 사용자가 직접 선택 |
| `/{ns}` | 네임스페이스 개요 (상단 메뉴 + Infra 셀렉터, 로고 없음) | ns 단위 전체 보기 |
| `/{ns}/{infra}` | **Infra 레벨 스코프 화면** (로고 없음, 셀렉터 없음) | iframe 임베드 |
| `/{ns}/{infra}/{node}` | **Node 레벨 스코프 화면** (로고 없음, 셀렉터 없음, 뒤로가기 버튼) | iframe 임베드 |
| `/console` | 개발/테스트 콘솔(토큰·ns·infra·node 수동 선택) | 로컬 디버깅 |
| `/embed/{section}/{ns}/...` | 메뉴 없는 단일 패널(예: `/embed/monitoring/{ns}/{infra}/{node}`) | 특정 패널 1개만 임베드 |
| `/embed/{section}` | 위와 같되 **ns를 URL에 넣지 않는 형태.** ns는 postMessage로 받아 `/embed/{section}/{ns}`로 자동 이동 | **포털 하위 메뉴별 임베드** |
| `/embed` | 메뉴 없는 네임스페이스 선택 화면 | ns·기능 모두 미지정 |

`{section}`은 `monitoring` · `logs` · `config` · `insight` · `alerts` · `trace` 여섯 개입니다. 이 중 어디에도 걸리지 않는 주소는 빈 화면 대신 **안내 화면**이 표시됩니다.

> 예시(원격): `http://20.41.115.17:18081/testns01/test01`, `http://20.41.115.17:18081/testns01/test01/vm-1`

---

## 2. 네임스페이스 스코프 화면 동작 (`/{ns}/{infra}`, `/{ns}/{infra}/{node}`)

iframe 임베드에 최적화된 화면입니다.

- **제품 로고("MC-Observability") 미표시.**
- **상단 메뉴(Monitoring/Logs/Config/Insight/Alerts/Tracing)는 표시**되며, 클릭하면 iframe 안에서 `/{section}/{ns}/{infra}[/{node}]`로 이동합니다. 이때 바뀌는 것은 **iframe 내부의 history**이고, 부모가 지정한 `src` 속성값 자체는 그대로입니다. 즉 부모는 아무것도 다시 하지 않아도 되지만, iframe 안의 현재 주소는 선택한 메뉴를 따라갑니다.
- **셀렉터 규칙**: 경로에 이미 들어간 식별자의 셀렉터는 숨깁니다. ns가 경로에 있으면 NS 셀렉터, infra가 경로에 있으면 Infra 셀렉터를 표시하지 않습니다. 따라서 `/{ns}/{infra}`·`/{ns}/{infra}/{node}` 화면에는 우측 상단 셀렉터가 없습니다.
- **Node 레벨(`/{ns}/{infra}/{node}`)**: 좌측 상단에 **뒤로가기(← Back) 버튼**이 있어 다시 Infra 레벨(`/{ns}/{infra}`)로 돌아갑니다.

---

## 3. 기본 임베드

```html
<!-- Infra 레벨 -->
<iframe src="http://<HOST>:18081/testns01/test01"
        style="width:100%;height:100%;border:0;"></iframe>

<!-- Node 레벨 -->
<iframe src="http://<HOST>:18081/testns01/test01/vm-1"
        style="width:100%;height:100%;border:0;"></iframe>
```

네임스페이스를 모를 때는 `src`를 `/`로 두면 사용자가 네임스페이스를 직접 고를 수 있습니다.

```html
<iframe src="http://<HOST>:18081/" style="width:100%;height:100%;border:0;"></iframe>
```

---

## 4. 네임스페이스 자동 감지 (부모 페이지 `#select-current-project`)

`src="/"`(네임스페이스 미지정)로 임베드한 경우, 프론트는 **iframe을 품은 부모 페이지**의 프로젝트 선택 `<select>`를 보고 네임스페이스를 자동으로 반영합니다.

- 부모 페이지에 **`id="select-current-project"`** 인 `<select>`가 있으면,
- 현재 **선택된 option의 "표시 텍스트"**(option의 `value`가 아니라 화면에 보이는 글자)를 읽어,
- 해당 네임스페이스 화면(`/{ns}`)으로 자동 이동합니다. 부모에서 선택을 바꾸면 이를 감지해 따라갑니다.

```html
<!-- 부모(임베드하는 쪽) 페이지 -->
<select id="select-current-project">
  <option value="prj-001">testns01</option>   <!-- 표시 텍스트 "testns01"이 namespace로 사용됨 -->
  <option value="prj-002" selected>testns02</option>
</select>
<iframe src="http://<HOST>:18081/" ...></iframe>
```

> **읽지 못하면 그냥 건너뜁니다(에러 아님).** iframe과 부모가 **다른 오리진**이면 브라우저 정책상 부모 DOM을 읽을 수 없습니다. 이때는 자동 감지를 조용히 포기하고, 사용자가 직접 고르는 **네임스페이스 선택 화면**으로 폴백합니다. (확실히 쓰려면 부모-iframe을 같은 오리진으로 두거나, 아래 postMessage 방식을 사용하세요.)

> 참고: 임베드용 첫 화면(`/`)에는 제품 로고와 Dev 버튼이 표시되지 않습니다.

---

## 5. 포털 하위 메뉴로 나눠서 임베드하기

포털 메뉴를 하나만 두고 그 안에서 프론트 상단 메뉴로 전환하는 대신, **포털 쪽에 기능별 하위 메뉴를 두고 각 메뉴가 자기 기능만 담긴 iframe을 띄우는** 방식입니다. 이 경우 프론트 상단 메뉴는 나오지 않으므로 포털 메뉴와 겹치지 않습니다.

### 5.1 하위 메뉴별 src

`/embed/{section}` 형태를 씁니다. **네임스페이스를 URL에 넣지 않아도 됩니다.**

| 포털 하위 메뉴 | iframe src |
|---|---|
| Monitoring | `http://<HOST>:18081/embed/monitoring` |
| Logs | `http://<HOST>:18081/embed/logs` |
| Config | `http://<HOST>:18081/embed/config` |
| Insight | `http://<HOST>:18081/embed/insight` |
| Alerts | `http://<HOST>:18081/embed/alerts` |
| Tracing | `http://<HOST>:18081/embed/trace` |

네임스페이스는 기존과 똑같이 **postMessage로 받습니다**(→ [6. 인증 토큰 전달](#6-인증-토큰-전달-postmessage)). 프론트가 ns를 받으면 스스로 `/embed/{section}/{ns}`로 이동하므로, 포털은 프로젝트가 바뀔 때마다 `src` 문자열을 다시 조립할 필요가 없습니다.

ns를 이미 알고 있다면 처음부터 `/embed/{section}/{ns}`로 지정해도 됩니다. 둘 다 동작합니다.

```html
<!-- 하위 메뉴 "Logs" 의 페이지 -->
<iframe id="o11y" src="http://<HOST>:18081/embed/logs"
        style="width:100%;height:100%;border:0;"></iframe>
<script>
  document.getElementById('o11y').onload = function () {
    this.contentWindow.postMessage({
      accessToken: "Bearer eyJhbGciOi...",
      workspaceInfo: { id: "...", name: "..." },
      projectInfo:   { id: "...", ns_id: "testns01", name: "..." }
    }, "http://<HOST>:18081");
  };
</script>
```

### 5.2 이때 보장되는 동작

- **프론트 상단 메뉴가 표시되지 않습니다.** `/embed/*`는 메뉴 없는 레이아웃을 씁니다.
- **워크스페이스·네임스페이스 선택은 상위 포털이 계속 담당합니다.** 하위 메뉴마다 별도로 고를 필요가 없습니다.
- **부모가 프로젝트(ns)를 바꾸면 보고 있던 기능을 유지한 채** 새 ns로 이동합니다. Logs를 보고 있었으면 `/embed/logs/{새ns}`로 갑니다. Monitoring으로 되돌아가지 않습니다.
- ns가 아직 안 왔거나 부모 없이 직접 열면, **그 기능으로 한정된 네임스페이스 선택 화면**이 표시됩니다.

> 포털이 프로젝트 변경 시 iframe을 통째로 다시 만드는 구조여도 됩니다. 새로 만들어진 iframe이 다시 `/embed/{section}`에서 시작해 postMessage로 ns를 받으므로 결과는 같습니다.

---

## 6. 인증 토큰 전달 (postMessage)

프론트는 부모 창에서 `window.postMessage`로 보내는 **액세스 토큰**을 받아 백엔드 호출에 사용합니다. (`AppContext`가 `message` 이벤트를 수신)

부모가 보내는 메시지 형식:

```js
const iframe = document.getElementById('o11y');
iframe.onload = () => {
  iframe.contentWindow.postMessage({
    accessToken: "Bearer eyJhbGciOi...",   // 필수: 이 값이 있어야 인증 처리됨
    projectInfo:   { ns_id: "testns01" },   // 선택: 네임스페이스 정보
    workspaceInfo: { /* ... */ }            // 선택
  }, "http://<HOST>:18081");                // targetOrigin: iframe origin과 일치시킬 것
};
```

- 수신측은 `accessToken`이 있는 메시지만 처리합니다(없으면 무시).
- 토큰은 이후 `/api/o11y/*` 호출의 `Authorization` 헤더로 사용됩니다.

### URL 파라미터로 토큰 전달(개발용)

postMessage 대신 쿼리스트링으로도 가능합니다.

```
http://<HOST>:18081/testns01/test01?token=Bearer%20eyJ...&ns_id=testns01
```

---

## 7. 백엔드 프록시 경로 (nginx)

프론트 nginx가 동일 오리진에서 백엔드로 프록시하므로 CORS 설정이 필요 없습니다.

| 프론트 경로 | 프록시 대상 |
|---|---|
| `/api/o11y/` | `mc-observability-manager:18080` (관측 매니저 API) |
| `/tumblebug/` | `mc-infra-manager:1323` (cb-tumblebug, Basic auth 자동 주입) |
| `/spider/` | `mc-infra-connector:1024` (cb-spider, Basic auth 자동 주입) |

SPA 라우팅은 `try_files $uri /index.html` 폴백으로 처리되므로 `/{ns}/{infra}/{node}` 같은 경로로 **직접 진입**해도 정상 동작합니다.

---

## 8. 참고 사항

- **에이전트 미설치 노드**: 메트릭이 없을 때 빈 "No data" 대신, **Config 메뉴에서 에이전트를 설치하라는 안내**가 표시됩니다.
- **`/embed/*`**: 상단 메뉴 없이 기능 하나만 임베드할 때 사용합니다. 포털 하위 메뉴로 나눌 때(→ 5장)와 대시보드 카드에 패널 하나만 넣을 때가 여기 해당합니다.
- 식별자 명명: cb-tumblebug 리네임에 맞춰 경로/필드는 `ns`(네임스페이스) · `infra`(구 MCI) · `node`(구 VM)를 사용합니다.
