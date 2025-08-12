from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent
from app.core.mcp.agent_state import ExtendedAgentState


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

