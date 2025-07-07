from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent


class OpenAIClient:
    def __init__(self, api_key: str):
        self.model = None
        self.api_key = api_key
        self.llm = None
        self.agent = None

    def setup(self, model='gpt-4o'):
        self.model = model
        self.llm = ChatOpenAI(model=self.model, temperature=0, api_key=self.api_key)

    def bind_tools(self, tools, memory):
        self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
        return self.agent

