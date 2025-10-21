# Chronograf 사용 가이드

## 1. 접속 안내
- chronograf는 mc-o11y가 설치된 URL의 8888 Port로 접속 가능합니다.
- **Ex:** `http://{mc-o11y-server url}:8888`

---

## 2. 초기 InfluxDB 연결 설정
- 초기 접속 시 influxdb 연결이 필요할 수 있습니다.
![img.png](docs/image/chronograf/img.png)
### a. 설정 값 입력
- **Connection URL:** `http://{mc-o11y-server url}:8086`
- **Connection Name:** 임의 값 (예: Influx 1)
- **Username, Password:** `mc-agent`, `mc-agent`
- **Telegraf Database Name:** `mc-observability`
- **Default Retention Policy:** 입력하지 않음
- **Default Connection:** 활성화

> 위 이미지와 같이 설정값 입력

---

### b. 추가 설정
- 다음 화면에서 별도 설정 없이 **Next** 클릭.
![img.png](docs/image/chronograf/img_1.png)

---

### c. Kapacitor Connection
![img.png](docs/image/chronograf/img_2.png)
- **SKIP** 버튼 클릭.

---

### d. 설정 완료
- 설정이 완료되었습니다.

---

## 3. 데이터 조회
- DB 연결이 완료되면 좌측 메뉴 중 **'Explore'** 에서 데이터 조회가 가능합니다.  
![img.png](docs/image/chronograf/img_3.png)

---

## 4. 데이터 조회 예시
- 아래 내용은 특정 VM의 `cpu:usage_idle` 최근 1시간 데이터를 1분 간격으로 조회하는 경우를 가정하고 있습니다.

---

## 5. DB 및 Measurement 선택
- **DB:** `mc-observability.autogen`
- **Measurement:** `cpu`
![img.png](docs/image/chronograf/img_4.png)

---

## 6. Tag 정보 확인
- **cpu** 클릭 시 Tag 정보가 표시됩니다.
- Tag 중 조회하고자 하는 VM의 ID 정보 (`ns`, `mci`, `target ID`)를 클릭합니다.
![img.png](docs/image/chronograf/img_5.png)
- 추가로 우측 필드 항목에서 모니터링 필드 항목을 확인할 수 있습니다.
![img.png](docs/image/chronograf/img_6.png)

---

## 7. 데이터 확인
- 데이터가 정상적으로 수집된 경우, 아래 이미지와 같이 상단 그래프 영역에서 값을 확인할 수 있습니다.
![img.png](docs/image/chronograf/img_7.png)
---

## 8. 데이터 조회 범위 설정
- 데이터 조회 범위는 우측 상단 UI에서 설정 가능합니다.
![img.png](docs/image/chronograf/img_8.png)
---

## 9. 데이터 간격 설정
- 데이터 간격은 우측 중간 **group by** 메뉴에서 선택 가능합니다.
![img.png](docs/image/chronograf/img_9.png)
---

## 10. 조회 결과 예시
- `ns01/mci01/g1-1-1`의 `cpu:usage_idle`을 1분 간격으로 총 1시간 조회한 결과는 아래와 같은 전체 화면으로 확인할 수 있습니다.
![img.png](docs/image/chronograf/img_10.png)
