from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from app.core.llm.google_client import GoogleClient
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
        elif provider == 'google':
            self.llm_client = GoogleClient(provider_credential)

        self.llm_client.setup(model=model_name)
        self.agent = self.llm_client.bind_tools(self.tools, self.memory)


    @staticmethod
    def _build_prompt(messages):
        current_time = datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")

        system_prompt = f"""
                You are an assistant specialized in log retrieval and log exploration.
                To respond to user questions, select the appropriate tools and provide thoughtful and sincere answers. 
                If the question is in English, respond in English; if it's in Korean, respond in Korean.
                The current time is {current_time}.
                """

        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": messages}
        ]

    async def aquery(self, session_id, messages):
        config = self.create_config(session_id)
        prompt = self._build_prompt(messages)
        response = await self.agent.ainvoke({'messages': prompt}, config=config)

        return response


    @staticmethod
    def create_config(session_id):
        return {'configurable': {'thread_id': f'{session_id}'}}

    async def get_chat_history(self, session_id):
        config = self.create_config(session_id)
        checkpoint = await self.memory.aget(config)
        return checkpoint
