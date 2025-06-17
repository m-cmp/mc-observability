from langchain_ollama import ChatOllama
from langchain_openai import ChatOpenAI

from langgraph.prebuilt import create_react_agent
from langgraph.checkpoint.memory import MemorySaver

import aiosqlite

class OllamaClient:
    def __init__(self):
        self.model = None
        self.base_url = None
        self.llm = None
        self.agent = None

    def setup(self, base_url='http://192.168.170.229:11434', model='llama3.1:8b'):
        self.base_url = base_url
        self.model = model
        self.llm = ChatOllama(base_url=self.base_url, model=self.model, temperature=0)

    def bind_tools(self, tools, memory):
        self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
        return self.agent
