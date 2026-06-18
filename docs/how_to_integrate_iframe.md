# iframe 연동 가이드 (How to integrate via iframe)

mc-observability 프론트(`mc-observability-front`, 기본 포트 `18081`)를 다른 콘솔/포털에 **iframe으로 임베드**하는 방법을 설명합니다. 네임스페이스 기반 URL만 넘기면 상단 메뉴로 모든 기능(Monitoring / Logs / Config / Insight / Alerts / Tracing)을 제어할 수 있습니다.

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

> 예시(원격): `http://20.41.115.17:18081/testns01/test01`, `http://20.41.115.17:18081/testns01/test01/vm-1`

---

## 2. 네임스페이스 스코프 화면 동작 (`/{ns}/{infra}`, `/{ns}/{infra}/{node}`)

iframe 임베드에 최적화된 화면입니다.

- **제품 로고("MC-Observability") 미표시.**
- **상단 메뉴(Monitoring/Logs/Config/Insight/Alerts/Tracing)는 표시**되며, 클릭 시 **URL을 바꾸지 않고** 화면 내용만 그 자리에서 전환됩니다(in-place). → iframe `src`가 고정 유지됩니다.
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

## 5. 인증 토큰 전달 (postMessage)

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

## 6. 백엔드 프록시 경로 (nginx)

프론트 nginx가 동일 오리진에서 백엔드로 프록시하므로 CORS 설정이 필요 없습니다.

| 프론트 경로 | 프록시 대상 |
|---|---|
| `/api/o11y/` | `mc-observability-manager:18080` (관측 매니저 API) |
| `/tumblebug/` | `mc-infra-manager:1323` (cb-tumblebug, Basic auth 자동 주입) |
| `/spider/` | `mc-infra-connector:1024` (cb-spider, Basic auth 자동 주입) |

SPA 라우팅은 `try_files $uri /index.html` 폴백으로 처리되므로 `/{ns}/{infra}/{node}` 같은 경로로 **직접 진입**해도 정상 동작합니다.

---

## 7. 참고 사항

- **에이전트 미설치 노드**: 메트릭이 없을 때 빈 "No data" 대신, **Config 메뉴에서 에이전트를 설치하라는 안내**가 표시됩니다.
- **`/embed/*`**: 상단 메뉴까지 빼고 패널 하나만 임베드하고 싶을 때 사용합니다(예: 대시보드 카드에 차트 하나만 넣기).
- 식별자 명명: cb-tumblebug 리네임에 맞춰 경로/필드는 `ns`(네임스페이스) · `infra`(구 MCI) · `node`(구 VM)를 사용합니다.
