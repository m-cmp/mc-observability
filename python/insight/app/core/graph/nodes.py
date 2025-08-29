import logging
from langchain_core.messages import SystemMessage
from .state import State
from .utils.summarization import ConversationSummarizer

logger = logging.getLogger(__name__)


async def call_model(state: State, llm=None) -> dict:
    """
    Main LLM call node that processes messages and generates responses.
    Always uses llm_input_messages for queries and updates responses back to it.
    
    Args:
        state: Current graph state containing messages and context
        llm: LLM client instance for generating responses
        
    Returns:
        dict: Updated state with new messages in both messages and llm_input_messages
    """
    messages = (state["llm_input_messages"] if state.get("is_summarized", False) else state["messages"])

    # LLM call
    response = await llm.ainvoke(messages)

    # Add response to both messages and llm_input_messages
    result = {"messages": [response]}

    if state.get("is_summarized", False):
        # Only add response to existing summary messages in summarized mode
        updated_llm_input_messages = state["llm_input_messages"] + [response]
        result["llm_input_messages"] = updated_llm_input_messages

    return result


async def summary_node(state: State, llm=None, config_manager=None) -> dict:
    """
    Summarization node that processes conversation history using RunningSummary.
    
    Args:
        state: Current graph state containing messages and context
        llm: LLM client for generating summaries
        config_manager: Configuration manager instance
        
    Returns:
        dict: Updated state with summarized messages
    """
    # Initialize required information
    # Get current messages and existing summary information
    is_summarized = state.get("is_summarized", False)
    messages = state.get("messages", [])
    context = state.get("context", {})
    current_summary = context.get("default")

    # Get config
    summarization_config = config_manager.get_chat_summarization_config()
    max_tokens = summarization_config.get("max_tokens_before_summary", 1024)
    summary_prompt = summarization_config.get("summary_prompt", "")

    # Initialize
    summarizer = ConversationSummarizer(max_tokens=max_tokens, summary_prompt=summary_prompt)

    try:
        # Execute summarization process
        updated_summary = await summarizer.process(messages, llm, is_summarized, current_summary)

        if updated_summary and updated_summary != current_summary:
            logger.info("Summary updated")

            # Convert summary to SystemMessage object with unique ID
            summary_system_message = SystemMessage(
                content=f"Previous conversation summary:\n{updated_summary.summary}",
                id=f"summary_{updated_summary.last_summarized_message_id}"
            )

            # Configure LLM input messages: SystemMessage + last 2 messages
            llm_input_messages = [summary_system_message] + messages[-2:]

            updated_context = context.copy()
            updated_context["default"] = updated_summary

            return {
                "messages": [summary_system_message],
                "llm_input_messages": llm_input_messages,
                "is_summarized": True,
                "context": updated_context
            }
        elif updated_summary and updated_summary == current_summary:
            logger.info("llm_input_messages updated")
            llm_input_messages = state.get("llm_input_messages", []) + messages[-2:]

            return {
                "llm_input_messages": llm_input_messages,
            }
        else:
            logger.debug("No summarization needed")
            return {
                "is_summarized": is_summarized
            }

    except Exception as e:
        logger.error(f"Error during summarization: {type(e).__name__}: {e}")
        return {
            "is_summarized": False
        }


async def tool_node(state: State) -> dict:
    """
    Tool execution node that handles function calls.
    
    Args:
        state: Current graph state containing messages and context
        
    Returns:
        dict: Updated state with tool results
    """
    # TODO: Implement tool execution logic
    return {}
