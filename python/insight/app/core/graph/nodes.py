import logging
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

    # LLM 호출
    response = await llm.ainvoke(messages)

    # 응답을 messages와 llm_input_messages 모두에 추가
    result = {"messages": [response]}

    if state.get("is_summarized", False):
        # 요약 모드일 때만 기존 요약 메시지에 response 추가
        updated_llm_input_messages = state["llm_input_messages"] + [response]
        result["llm_input_messages"] = updated_llm_input_messages

    return result


async def summary_node(state: State, llm=None, config_manager=None, context_key: str = "default") -> dict:
    """
    Summarization node that processes conversation history using RunningSummary.
    
    Args:
        state: Current graph state containing messages and context
        llm_client: LLM client for generating summaries
        config_manager: Configuration manager instance
        context_key: Key for storing/retrieving RunningSummary in state context
        
    Returns:
        dict: Updated state with summarized messages
    """
    if not llm or not config_manager:
        logger.warning("Required parameters missing for summary node")
        return {}

    # ConversationSummarizer 인스턴스 생성
    summarizer = ConversationSummarizer(config_manager)

    # 현재 메시지들과 기존 요약 정보 가져오기
    messages = state.get("messages", [])
    context = state.get("context", {})
    current_summary = context.get(context_key)

    try:
        # 요약 처리 실행
        updated_summary = await summarizer.process(messages, llm, current_summary)

        if updated_summary and updated_summary != current_summary:
            # 요약이 업데이트된 경우
            logger.info("Summary updated")

            # 시스템 프롬프트 기본값
            system_prompt = "You are a helpful AI assistant."

            # 요약을 고려한 프롬프트 구성 (사용자 메시지 없이)
            summarized_messages = summarizer.build_prompt_with_summary(
                messages, "", updated_summary, system_prompt
            )

            # 마지막 빈 사용자 메시지 제거
            if summarized_messages and summarized_messages[-1].get("content") == "":
                summarized_messages.pop()

            # context 업데이트
            updated_context = context.copy()
            updated_context[context_key] = updated_summary

            return {
                "llm_input_messages": summarized_messages,
                "is_summarized": True,
                "context": updated_context
            }
        else:
            logger.debug("No summarization needed")
            return {
                "is_summarized": False
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


async def clean_node(state: State) -> dict:
    """
    Cleanup node that removes unnecessary AI messages from history.
    
    Args:
        state: Current graph state containing messages and context
        
    Returns:
        dict: Updated state with cleaned message history
    """
    # TODO: Implement cleanup logic
    return {}
