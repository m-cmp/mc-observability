from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from config.ConfigManager import ConfigManager
from langchain_mcp_adapters.tools import load_mcp_tools
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver
# from langgraph.checkpoint.mysql.aio import AIOMySQLSaver
from datetime import datetime, UTC
import aiosqlite


class MCPContext:
    def __init__(self, mcp_client):
        self.config = ConfigManager()

        self.memory = AsyncSqliteSaver(aiosqlite.connect("checkpoints/checkpoints.sqlite", check_same_thread=False))

        # Todo
        # checkpointer MariaDB 사용 가능 여부 검증 필요
        # self._build_db_uri()
        # self.memory = AIOMySQLSaver.from_conn_string(self.uri)

        self.mcp_client = mcp_client
        self.mcp_session = None
        self.llm_client = None

        self.tools = None
        self.agent = None

    # Todo
    # def _build_db_uri(self):
    #     db_info = self.config.get_db_config()
    #     self.uri = f'mysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}'

    async def astart(self):
        self.mcp_session = await self.mcp_client.start()
        self.tools = await load_mcp_tools(self.mcp_session)

    async def astop(self):
        await self.mcp_client.stop()

    async def get_agent(self, provider: str, model_name: str, provider_credential: str):
        if provider == 'ollama':
            self.llm_client = OllamaClient(provider_credential)
        elif provider == 'openai':
            self.llm_client = OpenAIClient(provider_credential)

        self.llm_client.setup(model=model_name)
        self.agent = self.llm_client.bind_tools(self.tools, self.memory)

    async def _build_prompt(self, session_id: str, messages: str):
        history = await self.get_chat_history(session_id)
        msg_count = 0
        if history:
            channel_values = history.get('channel_values', {})
            msg_count = len(channel_values.get('messages', []))

        current_time = datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")

        # TODO 기능, 상황별 Prompt 관리 기능 추가
        system_prompt_config = self.config.get_system_prompt_config()

        if msg_count == 0:
            system_prompt_first = system_prompt_config.get("system_prompt_first")
            system_prompt = system_prompt_first.format(current_time=current_time)
        else:
            system_prompt_default = system_prompt_config.get("system_prompt_default")
            system_prompt = system_prompt_default.format(current_time=current_time)

        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": messages}
        ]

    async def aquery(self, session_id, messages):
        config = self.create_config(session_id)
        prompt = await self._build_prompt(session_id, messages)
        response = await self.agent.ainvoke({'messages': prompt}, config=config)
        return response

    @staticmethod
    def create_config(session_id):
        return {'configurable': {'thread_id': f'{session_id}'}}

    async def get_chat_history(self, session_id):
        config = self.create_config(session_id)
        checkpoint = await self.memory.aget(config)
        return checkpoint
