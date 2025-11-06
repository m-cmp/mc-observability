from langgraph.graph import MessagesState
from langmem.short_term import RunningSummary


class State(MessagesState):
    """Graph state containing conversation messages and context."""

    context: dict[str, RunningSummary]
    llm_input_messages: str
    is_summarized: bool
