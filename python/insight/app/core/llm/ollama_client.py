from langchain.agents import create_agent
from langchain_ollama import ChatOllama


class OllamaClient:
    def __init__(self, base_url=None):
        self.base_url = base_url
        self.model = None
        self.llm = None
        self.agent = None

    def setup(self, model: str):
        self.model = model
        self.llm = ChatOllama(base_url=self.base_url, model=self.model, temperature=0)

    def setup_graph_llm(self, model: str):
        self.model = model
        self.llm = ChatOllama(base_url=self.base_url, model=self.model, temperature=0)

    def create_agent_runner(
        self,
        tools,
        checkpointer=None,
        system_prompt: str | None = None,
        response_format=None,
        middleware=None,
    ):
        self.agent = create_agent(
            model=self.llm,
            tools=tools or [],
            system_prompt=system_prompt,
            checkpointer=checkpointer,
            response_format=response_format,
            middleware=middleware or [],
        )
        return self.agent

    def bind_tools(self, tools, memory):
        return self.create_agent_runner(tools=tools, checkpointer=memory)
