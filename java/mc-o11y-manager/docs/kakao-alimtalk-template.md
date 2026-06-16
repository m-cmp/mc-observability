# Kakao AlimTalk Notification Setup (NCP)

This guide explains how to enable KakaoTalk (AlimTalk / 알림톡) notifications for the
mc-observability trigger module on Naver Cloud Platform (NCP).

> **Why this is needed.** AlimTalk only delivers messages that match a template you have
> **pre-registered and gotten approved** in your own NCP account. The application does **not**
> ship the message text. Instead you register the template in NCP, and the application fetches
> that template by its `templateCode`, validates it, fills in the variables, and sends it.

## 1. Required configuration

Set these environment variables (see `config/manager/.env`):

| Variable | Description |
| --- | --- |
| `KAKAO_CHANNEL_ID` | Plus Friend / channel id (e.g. `@my-channel`) |
| `KAKAO_ALERT_TEMPLATE_CODE` | **Required.** templateCode of your registered *alert* template |
| `KAKAO_DIRECT_TEMPLATE_CODE` | **Required.** templateCode of your registered *direct* template |
| `KAKAO_SERVICE_ID` | NCP SENS service id (`ncp:kkobizmsg:kr:...`) |
| `KAKAO_ACCESS_KEY` | NCP IAM access key (must allow AlimTalk template inquiry + send) |
| `KAKAO_SECRET_KEY` | NCP IAM secret key |
| `KAKAO_BASEURL` | `https://sens.apigw.ntruss.com` |

If a template code is missing, Kakao notifications fail with a clear configuration error
(`Kakao templateCode is not configured`) instead of sending a wrong message.

## 2. Template variables (the catalog)

You may use the following `#{...}` placeholders in your registered template. **You choose the
template text, but the variable *names* must match this catalog exactly** — they are defined by
the application (in `KakaoTemplateVariable`), not by NCP. The application substitutes each
`#{name}` with the corresponding value before sending.

### Alert template (`KAKAO_ALERT_TEMPLATE_CODE`)

| Variable | Value |
| --- | --- |
| `#{title}` | Trigger policy name (Grafana rule group) |
| `#{alertCounts}` | Total alert count |
| `#{infoCount}` | INFO-level alert count |
| `#{warningCount}` | WARNING-level alert count |
| `#{criticalCount}` | CRITICAL-level alert count |
| `#{scope}` | Namespace id of the representative (highest-severity) alert |
| `#{targetId}` | VM id of the representative (highest-severity) alert |

> For an event that aggregates multiple resources, `#{scope}`/`#{targetId}` resolve to the
> highest-severity alert (CRITICAL &gt; WARNING &gt; INFO); they are empty when the event has no alerts.

### Direct template (`KAKAO_DIRECT_TEMPLATE_CODE`)

| Variable | Value |
| --- | --- |
| `#{title}` | Direct alert title |
| `#{message}` | Direct alert message body |

> If your registered template references a variable that is **not** in this catalog, the
> application fails fast at send time with the list of supported variables — it never sends a
> half-rendered message.

## 3. Recommended template content

Register these in the NCP console as-is (the variable placeholders use `#{...}`). The static
text is intentionally worded to pass AlimTalk inspection (see section 5).

**Alert template**

```
[M-CMP] 모니터링 알림

담당자님, 등록하신 트리거 정책 '#{title}'에서
설정한 임계치를 초과하여 알림이 발생했습니다.

▶ 발생 건수: 총 #{alertCounts}건
- info: #{infoCount}
- warning: #{warningCount}
- critical: #{criticalCount}
▶ 대표 대상: #{scope} / #{targetId}

본 메시지는 시스템 운영 담당자에게 발송되는 사내 업무용 알림입니다.
```

**Direct template**

```
[M-CMP] 시스템 알림

담당자님, 아래 운영 알림 내용을 확인해 주세요.

#{title}
#{message}

본 메시지는 시스템 운영 담당자에게 발송되는 사내 업무용 알림입니다.
```

## 4. Register the template in NCP

1. Open **NCP Console → Simple & Easy Notification Service (SENS) → KakaoTalk Channel**.
2. Register / connect your Plus Friend channel and note its id (`KAKAO_CHANNEL_ID`).
3. Create a new AlimTalk template, paste the content from section 3, and submit for inspection.
4. After approval, copy the `templateCode` into `KAKAO_ALERT_TEMPLATE_CODE` /
   `KAKAO_DIRECT_TEMPLATE_CODE`.
5. Make sure the template is approved and usable (`templateStatus` **ACTIVE** or **READY**). The
   application validates this on first use.

## 5. If your template is rejected (검수 반려)

AlimTalk only allows informational messages tied to a recipient action, and the reviewer must be
able to tell **who** receives it and **why**. The recommended content in section 3 already
addresses this by including `담당자님`, the triggering action (`등록하신 트리거 정책...`), and an
internal-use notice.

If asked to clarify during inspection, you can answer with:

```
본 메시지는 멀티클라우드 모니터링 플랫폼(M-CMP)의 시스템 알림입니다.
수신자는 시스템 운영 담당자(임직원)이며, 수신자가 직접 모니터링 트리거 정책을 등록하고
본인 연락처를 입력한 경우에 한해, 설정한 임계치 초과 시 자동 발송되는 사내 업무용 정보성 메시지입니다.
```

## 6. How validation works

On the first send for each template code, the application calls the NCP AlimTalk template
inquiry API and verifies:

- the template **exists** for the configured `channelId`,
- `templateStatus` is usable (**ACTIVE** or **READY**),
- the template **content is not empty**, and
- every `#{...}` placeholder is a **supported variable** (section 2).

The result is cached, so the inquiry API is not called on every notification. If you later change
the template content in NCP, evict/restart to re-fetch; a mismatch surfaces as a delivery failure
recorded in the notification history.
