from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from config.ConfigManager import ConfigManager
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver

# from langgraph.checkpoint.mysql.aio import AIOMySQLSaver
from datetime import datetime, UTC
import aiosqlite


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
        config = self.create_config(session_id)
        prompt = await self._build_prompt(session_id, messages)
        response = await self.agent.ainvoke({"messages": prompt}, config=config)
        return response

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
