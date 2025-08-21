# generate_bulk_data.py
import requests
import random
import time

# --- 설정 ---
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_USER = "mc-agent"
INFLUXDB_PASSWORD = "mc-agent"
TARGET_DB = "mc-observability"
BATCH_SIZE = 500  # 한 번에 보낼 데이터 개수
TOTAL_POINTS = 5000  # 생성할 총 데이터 개수
# ------------


def write_to_influxdb_batch(data_points):
    """데이터를 배치 단위로 InfluxDB에 기록합니다."""
    write_url = f"{INFLUXDB_URL}/write"
    params = {"db": TARGET_DB, "u": INFLUXDB_USER, "p": INFLUXDB_PASSWORD}

    for i in range(0, len(data_points), BATCH_SIZE):
        batch = data_points[i : i + BATCH_SIZE]
        payload = "\n".join(batch)

        try:
            response = requests.post(
                write_url, params=params, data=payload.encode("utf-8")
            )
            response.raise_for_status()
            print(
                f"성공: {len(batch)}개의 데이터 포인트를 성공적으로 기록했습니다. (진행률: {i + len(batch)}/{len(data_points)})"
            )
        except requests.exceptions.RequestException as e:
            print(f"오류 발생: {e}")
            if e.response:
                print(f"응답 내용: {e.response.text}")
            return
        time.sleep(0.1)  # 서버에 부담을 주지 않기 위한 약간의 딜레이


# --- 대량 데이터 생성 로직 ---
def generate_data():
    print(f"총 {TOTAL_POINTS}개의 샘플 데이터 생성을 시작합니다...")
    hosts = ["app-server-01", "app-server-02", "db-server-01", "cache-server-01"]
    points = []

    for i in range(TOTAL_POINTS):
        host = random.choice(hosts)
        # 5번에 1번 꼴로 CPU 스파이크(90% 이상) 발생 시뮬레이션
        cpu_usage = (
            random.uniform(90.0, 99.9)
            if random.randint(1, 5) == 1
            else random.uniform(10.0, 85.0)
        )
        memory_usage = random.randint(1024, 8192)

        # 현재 시간 기준으로 과거 데이터인 것처럼 타임스탬프 추가 (나노초)
        timestamp = int(time.time_ns()) - (i * 10**9)  # 1초씩 과거로

        points.append(f"cpu,host={host} usage_percent={cpu_usage:.2f} {timestamp}")
        points.append(f"memory,host={host} usage_mb={memory_usage} {timestamp}")

    return points


if __name__ == "__main__":
    all_data_points = generate_data()
    write_to_influxdb_batch(all_data_points)
    print("모든 데이터 생성이 완료되었습니다.")
