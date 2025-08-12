from langchain_ollama import ChatOllama
from langgraph.prebuilt import create_react_agent
from app.core.mcp.agent_state import ExtendedAgentState


class OllamaClient:
    def __init__(self, base_url='http://192.168.170.229:11434'):
        self.base_url = base_url
        self.model = None
        self.llm = None
        self.agent = None

    def setup(self, model='llama3.1:8b'):
        self.model = model
        self.llm = ChatOllama(base_url=self.base_url, model=self.model, temperature=0)

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
