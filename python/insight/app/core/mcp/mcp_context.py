import logging
import re
import time

# from langgraph.checkpoint.mysql.aio import AIOMySQLSaver
from datetime import UTC, datetime

import aiosqlite
from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from config.ConfigManager import ConfigManager
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver

logger = logging.getLogger(__name__)


class MCPContext:
    def __init__(self, mcp_manager):
        self.config = ConfigManager()

        self.memory = AsyncSqliteSaver(aiosqlite.connect("checkpoints/checkpoints.sqlite", check_same_thread=False))

        # Todo
        # checkpointer MariaDB 사용 가능 여부 검증 필요
        # self._build_db_uri()
        # self.memory = AIOMySQLSaver.from_conn_string(self.uri)

        self.mcp_manager = mcp_manager
        self.llm_client = None
        self.tools = None
        self.agent = None

        # 쿼리 메타데이터 추적을 위한 변수들
        self.query_metadata = {"queries_executed": [], "total_execution_time": 0.0, "tool_calls_count": 0, "databases_accessed": set()}

    def reset_metadata(self):
        """새로운 쿼리 시작 시 메타데이터 초기화"""
        self.query_metadata = {"queries_executed": [], "total_execution_time": 0.0, "tool_calls_count": 0, "databases_accessed": set()}

    def track_query_execution(self, query: str, execution_time: float, database: str = None):
        """쿼리 실행 정보를 추적"""
        self.query_metadata["queries_executed"].append(query)
        self.query_metadata["total_execution_time"] += execution_time
        self.query_metadata["tool_calls_count"] += 1

        if database:
            self.query_metadata["databases_accessed"].add(database)

    def get_metadata_summary(self):
        """현재까지 수집된 메타데이터를 반환"""
        return {
            "queries_executed": self.query_metadata["queries_executed"],
            "total_execution_time": round(self.query_metadata["total_execution_time"], 3),
            "tool_calls_count": self.query_metadata["tool_calls_count"],
            "databases_accessed": list(self.query_metadata["databases_accessed"]),
        }

    # Todo
    # def _build_db_uri(self):
    #     db_info = self.config.get_db_config()
    #     self.uri = f'mysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}'

    async def get_agent(self, provider: str, model_name: str, provider_credential: str):
        if provider == "ollama":
            self.llm_client = OllamaClient(provider_credential)
        elif provider == "openai":
            self.llm_client = OpenAIClient(provider_credential)

        self.llm_client.setup(model=model_name)

        self.tools = self.mcp_manager.get_all_tools()
        logger.info(f"Using {len(self.tools)} tools from multi-MCP environment")

        self.agent = self.llm_client.bind_tools(self.tools, self.memory)

    async def _build_prompt(self, session_id: str, messages: str):
        history = await self.get_chat_history(session_id)
        msg_count = 0
        if history:
            channel_values = history.get("channel_values", {})
            msg_count = len(channel_values.get("messages", []))

        current_time = datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")

        # TODO 기능, 상황별 Prompt 관리 기능 추가
        system_prompt_config = self.config.get_system_prompt_config()

        if msg_count == 0:
            system_prompt_first = system_prompt_config.get("system_prompt_first")
            system_prompt = system_prompt_first.format(current_time=current_time)
        else:
            system_prompt_default = system_prompt_config.get("system_prompt_default")
            system_prompt = system_prompt_default.format(current_time=current_time)

        return [{"role": "system", "content": system_prompt}, {"role": "user", "content": messages}]

    async def aquery(self, session_id, messages):
        self.reset_metadata()
        start_time = time.time()

        config = self.create_config(session_id)
        prompt = await self._build_prompt(session_id, messages)

        response = await self.agent.ainvoke({"messages": prompt}, config=config)

        total_time = time.time() - start_time
        self.query_metadata["total_execution_time"] = round(total_time, 3)

        self._extract_tool_calls_from_response(response)

        return response

    def _extract_tool_calls_from_response(self, response):
        """LangGraph 응답에서 도구 호출 정보를 추출.
        정책 변경: queries_executed에는 순수 쿼리 문자열만 포함.
        - message.content에서의 정규식 추출은 비활성화
        - tool_calls.args 내 query/influxql_query/sql_query 키에서만 수집
        - 좌우 공백 제거 및 중복 제거
        """
        try:
            messages = response.get("messages", [])
            logger.info(f"Extracting from {len(messages)} messages")

            for message in messages:
                logger.info(f"Message type: {getattr(message, 'type', 'unknown')}")

                if hasattr(message, "type") and message.type == "tool":
                    self.query_metadata["tool_calls_count"] += 1
                    logger.info(f"Found tool call, count: {self.query_metadata['tool_calls_count']}")

                elif hasattr(message, "type") and message.type == "ai":
                    if hasattr(message, "tool_calls") and message.tool_calls:
                        tool_calls = message.tool_calls
                        try:
                            tc_len = len(tool_calls)
                        except Exception:
                            tc_len = 0
                        self.query_metadata["tool_calls_count"] += tc_len
                        logger.info(f"Found {tc_len} tool calls in AI message")

                        for tool_call in tool_calls:
                            call_name = None
                            if isinstance(tool_call, dict):
                                call_name = tool_call.get("name")
                                if not call_name:
                                    fn = tool_call.get("function")
                                    if isinstance(fn, dict):
                                        call_name = fn.get("name")
                            elif hasattr(tool_call, "name"):
                                call_name = tool_call.name

                            inferred_db = None
                            if isinstance(call_name, str):
                                name_lower = call_name.lower()
                                influx_tools = {
                                    "execute_influxql",
                                    "get_time_window_summary",
                                    "get_last_data_point_timestamp",
                                    "get_measurement_schema",
                                    "get_tag_values",
                                    "list_measurements",
                                    "list_influxdb_databases",
                                }
                                maria_tools = {
                                    "execute_sql",
                                    "query_sql",
                                    "list_databases",
                                    "list_tables",
                                }
                                if ("influx" in name_lower) or (call_name in influx_tools):
                                    inferred_db = "InfluxDB"
                                elif ("maria" in name_lower) or ("mysql" in name_lower) or (call_name in maria_tools):
                                    inferred_db = "MariaDB"

                            args_dict = None
                            if isinstance(tool_call, dict):
                                args_dict = tool_call.get("args") or tool_call.get("arguments")
                                if args_dict is None:
                                    fn = tool_call.get("function")
                                    if isinstance(fn, dict):
                                        args_dict = fn.get("arguments")
                            elif hasattr(tool_call, "args"):
                                args_val = getattr(tool_call, "args")
                                if args_val is not None:
                                    args_dict = args_val

                            if isinstance(args_dict, str):
                                try:
                                    import json as _json

                                    parsed = _json.loads(args_dict)
                                    if isinstance(parsed, dict):
                                        args_dict = parsed
                                except Exception:
                                    args_dict = None

                            if isinstance(args_dict, dict):
                                captured_any_query_for_call = False
                                for key, value in args_dict.items():
                                    if key and isinstance(key, str) and key.lower() in ("query", "influxql_query", "sql_query") and isinstance(value, str):
                                        q = value.strip()
                                        q = re.sub(r"\s+", " ", q)
                                        if q and q not in self.query_metadata["queries_executed"]:
                                            self.query_metadata["queries_executed"].append(q)
                                            captured_any_query_for_call = True
                                            logger.info(f"Captured query: {q[:160]}...")

                                if captured_any_query_for_call and inferred_db:
                                    self.query_metadata["databases_accessed"].add(inferred_db)

            logger.info(f"Final metadata: {self.query_metadata}")

        except Exception as e:
            logger.error(f"Error extracting tool calls: {e}")
            import traceback

            traceback.print_exc()

    @staticmethod
    def create_config(session_id):
        return {"configurable": {"thread_id": f"{session_id}"}}

    async def get_chat_history(self, session_id):
        config = self.create_config(session_id)
        checkpoint = await self.memory.aget(config)
        return checkpoint
