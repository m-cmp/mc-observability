from langchain_ollama import ChatOllama
from langgraph.prebuilt import create_react_agent


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

    def bind_tools(self, tools, memory):
        self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
        return self.agent
