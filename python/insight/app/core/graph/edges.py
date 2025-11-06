from .state import State


def should_continue(state: State) -> str:
    """
    Determines whether to continue with tool execution or end the conversation.

    Args:
        state: Current graph state containing messages and context

    Returns:
        str: 'continue' to execute tools, 'end' to finish conversation
    """
    if state.get("is_summarized", False):
        messages = state["llm_input_messages"]
    else:
        messages = state["messages"]

    if not messages:
        return "end"

    last_message = messages[-1]

    if hasattr(last_message, "tool_calls") and last_message.tool_calls:
        return "continue"
    else:
        return "end"


def should_summarize(state: State) -> str:
    """
    Determines whether summarization is needed based on message count/tokens.

    Args:
        state: Current graph state containing messages and context

    Returns:
        str: 'summarize' to run summarization, 'proceed' to continue
    """
    # TODO: Implement summarization trigger logic
    return "proceed"
