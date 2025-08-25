from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent


class OpenAIClient:
    def __init__(self, api_key: str, base_url=None):
        self.model = None
        self.api_key = api_key
        self.base_url = base_url
        self.llm = None
        self.agent = None

    def setup(self, model):
        self.model = model
        if self.base_url:
            self.llm = ChatOpenAI(model=self.model, api_key=self.api_key, base_url=self.base_url)
        else:
            self.llm = ChatOpenAI(model=self.model, api_key=self.api_key)

    def bind_tools(self, tools, memory):
        self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
        return self.agent
