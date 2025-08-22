from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from config.ConfigManager import ConfigManager
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver

# from langgraph.checkpoint.mysql.aio import AIOMySQLSaver
from datetime import datetime, UTC
import aiosqlite
import time
import re


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

        # 다중 MCP 환경에서 모든 도구를 가져옴
        self.tools = self.mcp_manager.get_all_tools()
        print(f"[DEBUG] Using {len(self.tools)} tools from multi-MCP environment")

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
        # 메타데이터 초기화
        self.reset_metadata()
        start_time = time.time()

        config = self.create_config(session_id)
        prompt = await self._build_prompt(session_id, messages)

        # 에이전트 실행 및 도구 호출 추적
        response = await self.agent.ainvoke({"messages": prompt}, config=config)

        # 총 실행 시간 계산
        total_time = time.time() - start_time
        self.query_metadata["total_execution_time"] = round(total_time, 3)

        # 응답에서 도구 호출 정보 추출 (LangGraph의 tool_calls 메시지 분석)
        self._extract_tool_calls_from_response(response)

        return response

    def _extract_tool_calls_from_response(self, response):
        """LangGraph 응답에서 도구 호출 정보를 추출"""
        try:
            messages = response.get("messages", [])
            print(f"[DEBUG] Extracting from {len(messages)} messages")

            for message in messages:
                print(f"[DEBUG] Message type: {getattr(message, 'type', 'unknown')}")

                # tool 타입 메시지 처리
                if hasattr(message, "type") and message.type == "tool":
                    self.query_metadata["tool_calls_count"] += 1
                    print(f"[DEBUG] Found tool call, count: {self.query_metadata['tool_calls_count']}")

                    # 도구 호출 결과에서 쿼리 추출
                    if hasattr(message, "content"):
                        content = str(message.content)
                        self._extract_queries_from_content(content)

                # ai 타입 메시지에서도 쿼리 패턴 찾기
                elif hasattr(message, "type") and message.type == "ai":
                    if hasattr(message, "content"):
                        content = str(message.content)
                        self._extract_queries_from_content(content)

                    # tool_calls 속성이 있는 경우
                    if hasattr(message, "tool_calls") and message.tool_calls:
                        self.query_metadata["tool_calls_count"] += len(message.tool_calls)
                        print(f"[DEBUG] Found {len(message.tool_calls)} tool calls in AI message")

                        for tool_call in message.tool_calls:
                            # 도구 이름 기반으로 데이터베이스 추적
                            if hasattr(tool_call, "name"):
                                if "influx" in tool_call.name.lower():
                                    self.query_metadata["databases_accessed"].add("InfluxDB")
                                elif "maria" in tool_call.name.lower() or "mysql" in tool_call.name.lower():
                                    self.query_metadata["databases_accessed"].add("MariaDB")

                            # 도구 호출 인자에서 쿼리 추출
                            if hasattr(tool_call, "args") and isinstance(tool_call.args, dict):
                                for key, value in tool_call.args.items():
                                    if key.lower() in ["query", "influxql_query", "sql_query"] and isinstance(value, str):
                                        self.query_metadata["queries_executed"].append(value.strip())
                                        print(f"[DEBUG] Extracted query from args: {value[:100]}...")

            print(f"[DEBUG] Final metadata: {self.query_metadata}")

        except Exception as e:
            print(f"[DEBUG] Error extracting tool calls: {e}")
            import traceback

            traceback.print_exc()

    def _extract_queries_from_content(self, content: str):
        """컨텐츠에서 SQL/InfluxQL 쿼리를 추출"""
        try:
            # InfluxQL 쿼리 패턴
            influx_patterns = [
                r"SHOW\s+DATABASES",
                r"SHOW\s+MEASUREMENTS",
                r"SELECT.*?FROM.*?(?:WHERE.*?)?(?:GROUP BY.*?)?(?:LIMIT.*?)?(?:;|$)",
                r"SHOW\s+TAG\s+VALUES.*?FROM",
                r"SHOW\s+FIELD\s+KEYS.*?FROM",
            ]

            for pattern in influx_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE | re.DOTALL)
                for match in matches:
                    cleaned_query = re.sub(r"\s+", " ", match.strip())
                    if cleaned_query and cleaned_query not in self.query_metadata["queries_executed"]:
                        self.query_metadata["queries_executed"].append(cleaned_query)
                        self.query_metadata["databases_accessed"].add("InfluxDB")
                        print(f"[DEBUG] Found InfluxQL query: {cleaned_query[:100]}...")

            # MariaDB/MySQL 쿼리 패턴
            maria_patterns = [r"(?:SELECT|INSERT|UPDATE|DELETE|SHOW).*?(?:FROM|INTO|TABLE).*?(?:;|$)", r"DESCRIBE\s+\w+", r"SHOW\s+TABLES"]

            for pattern in maria_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE | re.DOTALL)
                for match in matches:
                    if "FROM" in match.upper() or "INTO" in match.upper() or "SHOW" in match.upper():
                        cleaned_query = re.sub(r"\s+", " ", match.strip())
                        if cleaned_query and cleaned_query not in self.query_metadata["queries_executed"]:
                            self.query_metadata["queries_executed"].append(cleaned_query)
                            self.query_metadata["databases_accessed"].add("MariaDB")
                            print(f"[DEBUG] Found MariaDB query: {cleaned_query[:100]}...")

        except Exception as e:
            print(f"[DEBUG] Error extracting queries from content: {e}")

    async def aquery_stream(self, session_id, messages):
        """스트리밍 방식으로 쿼리 응답을 생성합니다."""
        config = self.create_config(session_id)
        prompt = await self._build_prompt(session_id, messages)

        # LangGraph 에이전트의 스트리밍 응답
        async for chunk in self.agent.astream({"messages": prompt}, config=config):
            # 에이전트 스트림에서 메시지 내용 추출
            if "messages" in chunk and chunk["messages"]:
                last_message = chunk["messages"][-1]
                if hasattr(last_message, "content") and last_message.content:
                    yield last_message.content

    @staticmethod
    def create_config(session_id):
        return {"configurable": {"thread_id": f"{session_id}"}}

    async def get_chat_history(self, session_id):
        config = self.create_config(session_id)
        checkpoint = await self.memory.aget(config)
        return checkpoint
