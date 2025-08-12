from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent
from app.core.mcp.agent_state import ExtendedAgentState


class OpenAIClient:
    def __init__(self, api_key: str, base_url=None):
        self.model = None
        self.api_key = api_key
        self.base_url = base_url
        self.llm = None
        self.agent = None

    def setup(self, model, streaming=False):
        self.model = model
        if self.base_url:
            self.llm = ChatOpenAI(model=self.model, api_key=self.api_key, base_url=self.base_url, streaming=streaming)
        else:
            self.llm = ChatOpenAI(model=self.model, api_key=self.api_key, streaming=streaming)

    def bind_tools(self, tools, memory):
        self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
        return self.agent

    def bind_tools_with_summarization(self, tools, memory, summarization_node):
        """요약 기능이 포함된 Agent 생성"""
        self.agent = create_react_agent(
            model=self.llm,
            tools=tools,
            checkpointer=memory,
            pre_model_hook=summarization_node,
            state_schema=ExtendedAgentState,
        )
        return self.agent

