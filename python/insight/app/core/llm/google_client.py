from langchain_google_genai import ChatGoogleGenerativeAI
from langgraph.prebuilt import create_react_agent


class GoogleClient:
    def __init__(self, api_key: str):
        self.model = None
        self.api_key = api_key
        self.llm = None
        self.agent = None

    def setup(self, model='gemini-2.0-flash'):
        self.model = model
        self.llm = ChatGoogleGenerativeAI(model=model, google_api_key=self.api_key.API_KEY, temperature=0)

    def bind_tools(self, tools, memory):
        self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
        return self.agent