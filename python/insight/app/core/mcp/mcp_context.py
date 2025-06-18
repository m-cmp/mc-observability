from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient

from langchain_mcp_adapters.tools import load_mcp_tools

from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver

from datetime import datetime, UTC
import aiosqlite


class MCPContext:
    def __init__(self, mcp_client):
        self.memory = AsyncSqliteSaver(aiosqlite.connect("checkpoints/checkpoints.sqlite", check_same_thread=False))

        self.mcp_client = mcp_client
        self.mcp_session = None
        self.llm_client = None

        self.tools = None
        self.agent = None

    async def astart(self):
        self.mcp_session = await self.mcp_client.start()
        self.tools = await load_mcp_tools(self.mcp_session)

    async def astop(self):
        await self.mcp_client.stop()

    async def get_agent(self, provider, model_name):
        if provider == 'ollama':
            self.llm_client = OllamaClient()
        elif provider == 'openai':
            self.llm_client = OpenAIClient()

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
        # self.tools = await self.mcp_client.get_tools()
        # self.agent = self.llm_client.setup(self.tools, self.memory)
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
