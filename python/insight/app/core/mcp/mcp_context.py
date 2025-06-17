from app.core.llm.ollama_client import OllamaClient
from app.core.mcp.mcp_grafana_client import MCPGrafanaClient

from langchain_mcp_adapters.tools import load_mcp_tools

from datetime import datetime, UTC


class MCPContext:
    def __init__(self, mcp_client, llm_client):
        self.mcp_client = mcp_client
        self.mcp_session = None

        self.llm_client = llm_client

        self.tools = None
        self.agent = None
        self.memory = None

    async def start(self):
        self.mcp_session = await self.mcp_client.start()
        # self.tools = await self.mcp_client.get_tools()
        self.tools = await load_mcp_tools(self.mcp_session)

        self.agent, self.memory = self.llm_client.setup(self.tools)

    async def stop(self):
        await self.mcp_client.stop()

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

    async def query(self, messages, user_id, session_id):
        config = self.get_config(user_id, session_id)
        prompt = self._build_prompt(messages)
        response = await self.agent.ainvoke({'messages': prompt}, config=config)

        return response


    @staticmethod
    def get_config(user_id, session_id):
        return {'configurable': {'thread_id': f'{user_id}_{session_id}'}}



