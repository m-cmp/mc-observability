from langchain_openai import ChatOpenAI

from langgraph.prebuilt import create_react_agent


class OpenAIClient:
    def __init__(self):
        self.model = None
        self.base_url = None
        self.llm = None
        self.agent = None

    def setup(self, model='gpt-4o'):
        self.model = model
        self.llm = ChatOpenAI(model=self.model, temperature=0)

    def bind_tools(self, tools, memory):
        self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
        return self.agent

