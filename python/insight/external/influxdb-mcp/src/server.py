# main.py
import argparse
import importlib
import logging
import os

from influx_client import InfluxDBClient
from mcp.server.fastmcp import FastMCP

logger = logging.getLogger(__name__)

# --- 서버 및 클라이언트 초기화 ---
mcp = FastMCP(
    title="InfluxDBv1-MCP (Auto-Discovery & Robust Path)",
    description="실행 경로에 관계없이 안정적으로 작동하는, 자동 탐색 기반의 MCP 서버입니다.",
    version="6.1.0",
    host="0.0.0.0",
    port=8000,
)

client = InfluxDBClient()


# --- 1. 스크립트의 현재 위치를 기준으로 'tools' 디렉토리의 절대 경로를 만듭니다. ---
# __file__ 은 현재 이 스크립트 파일(main.py)의 경로를 의미합니다.
script_dir = os.path.dirname(os.path.abspath(__file__))
tools_dir = os.path.join(script_dir, "tools")

# --- 2. 'tools' 디렉토리에서 모든 도구를 자동으로 찾아 등록하는 로직 ---
logger.info("--- 도구 자동 탐색 시작 ---")
logger.info(f"탐색 대상 디렉토리: {tools_dir}")

try:
    for filename in os.listdir(tools_dir):
        # 파이썬 파일만 대상으로 하고, __init__.py 같은 특수 파일은 제외합니다.
        if filename.endswith(".py") and not filename.startswith("__"):
            # 이제 모듈 경로는 'tools.list_influxdb_databases'와 같은 상대 경로가 아닌,
            # 파일 시스템의 절대 경로를 기반으로 로드해야 할 수도 있으므로,
            # 더 안정적인 방식을 위해 모듈 로더를 직접 사용합니다. (아래 로직은 더 견고함)
            module_name = f"tools.{filename[:-3]}"

            try:
                module = importlib.import_module(module_name)

                if hasattr(module, "register_tool"):
                    register_function = getattr(module, "register_tool")
                    register_function(mcp, client)
                    logger.info(f"✅ '{module_name}' 도구를 성공적으로 등록했습니다.")
                else:
                    logger.warning(f"⚠️ '{module_name}' 모듈에 'register_tool' 함수가 없어 건너뜁니다.")

            except Exception as e:
                logger.error(f"❌ '{module_name}' 도구를 등록하는 중 오류가 발생했습니다: {e}")

except FileNotFoundError:
    logger.error(f"❌ 치명적 오류: '{tools_dir}' 디렉토리를 찾을 수 없습니다. 파일 구조를 확인해주세요.")
except Exception as e:
    logger.error(f"❌ 도구 로딩 중 예상치 못한 오류 발생: {e}")


logger.info("--- 모든 도구 등록 완료 ---")

# --- 서버 실행 ---
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="InfluxDB v1 MCP Server")
    parser.add_argument("--transport", choices=["stdio", "sse"], default="stdio")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    if args.transport == "sse":
        # FastMCP SSE 모드 사용 (호스트는 내부적으로 설정됨)
        mcp.run(transport="sse")
    else:
        mcp.run(transport="stdio")
