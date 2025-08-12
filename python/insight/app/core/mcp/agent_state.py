from langgraph.prebuilt.chat_agent_executor import AgentState
from langmem.short_term import RunningSummary


class ExtendedAgentState(AgentState):
    """
    MCPContext용 확장된 AgentState
    요약 컨텍스트를 추가하여 대화 히스토리 관리
    """
    # 요약 정보를 추적하여 매번 요약하지 않도록 함
    context: dict[str, RunningSummary]