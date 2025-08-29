import logging
from typing import List, Dict, Optional
from langchain_core.messages.utils import count_tokens_approximately
from langmem.short_term import RunningSummary

logger = logging.getLogger(__name__)


class ConversationSummarizer:
    """Conversation summarization management class"""

    def __init__(self, max_tokens: int, summary_prompt: str):
        self.max_tokens = max_tokens
        self.summary_prompt = summary_prompt

    def should_summarize(self, messages: List, running_summary: Optional[RunningSummary] = None,
                         is_summarized: bool = False) -> bool:
        """Determine if summarization is needed based on token count"""
        # First conversation pass: skip summarization for 2 messages
        if len(messages) == 2:
            logger.debug("First conversation pass: skipping summarization for 2 messages")
            return False

        if is_summarized:
            # When is_summarized is True, calculate tokens only for messages after summarization point
            messages_to_count = []
            for msg in messages:
                msg_id = None
                if hasattr(msg, 'id') and msg.id:
                    msg_id = str(msg.id)
                elif hasattr(msg, 'additional_kwargs') and 'id' in msg.additional_kwargs:
                    msg_id = str(msg.additional_kwargs['id'])

                # Include only unsummarized messages
                if msg_id and msg_id not in running_summary.summarized_message_ids:
                    messages_to_count.append(msg)
                elif not msg_id:  # Messages without ID are considered new messages
                    messages_to_count.append(msg)

            history_tokens = count_tokens_approximately(messages_to_count)
            logger.debug(
                f"Token count after summary: {history_tokens} (from {len(messages_to_count)} unsummarized messages)")
        else:
            # When is_summarized is False, calculate tokens for all messages
            history_tokens = count_tokens_approximately(messages)
            logger.debug(f"Token count (not summarized): {history_tokens} (from {len(messages)} total messages)")

        should_summarize = history_tokens > self.max_tokens
        logger.debug(f"Token threshold: {self.max_tokens}, should_summarize: {should_summarize}")

        return should_summarize

    async def create_summary(self, messages_to_summarize: List, llm) -> str:
        """Summarize specified messages"""
        logger.info(f"Starting to summarize {len(messages_to_summarize)} messages")

        # TODO Refactor this
        # Convert messages to text
        messages_text = ""
        for i, msg in enumerate(messages_to_summarize):
            if hasattr(msg, 'content'):
                content = msg.content
            elif isinstance(msg, dict):
                content = msg.get('content', str(msg))
            else:
                content = str(msg)
            messages_text += f"[{i + 1}] {content}\n"

        formatted_prompt = self.summary_prompt.format(messages=messages_text)

        try:
            # Generate summary using LLM directly
            summary_response = await llm.ainvoke([{"role": "user", "content": formatted_prompt}])
            summary = summary_response.content.strip()

            logger.info(f"Summary created successfully (length: {len(summary)} characters)")
            logger.debug(f"Summary preview: {summary[:100]}...")

            return summary

        except Exception as e:
            logger.error(f"Summary creation failed: {type(e).__name__}: {e}")
            return ""

    @staticmethod
    def update_running_summary(current_summary: Optional[RunningSummary], new_summary: str,
                               messages_to_summarize: List, processed_message_ids: List[str]) -> RunningSummary:
        """Update RunningSummary object"""
        # Use all processed message IDs (including tool/system)
        processed_ids = set(processed_message_ids)
        last_message_id = processed_message_ids[-1] if processed_message_ids else None

        if current_summary and current_summary.summary:
            # If existing summary exists - accumulate all processed IDs
            combined_ids = current_summary.summarized_message_ids.union(processed_ids)
        else:
            # First summary - save all processed IDs
            combined_ids = processed_ids

        logger.debug("Updating running summary state")
        logger.debug(f"Summary length: {len(new_summary)} characters")
        logger.debug(f"Summarized message ID count: {len(combined_ids)}")
        logger.debug(f"Last summarized message ID: {last_message_id}")

        return RunningSummary(
            summary=new_summary,
            summarized_message_ids=combined_ids,
            last_summarized_message_id=last_message_id
        )

    async def process(self, messages: List, llm, is_summarized: bool,
                      running_summary: Optional[RunningSummary] = None) -> Optional[RunningSummary]:
        """Execute summarization process - returns RunningSummary object"""
        # Exclude current input message processing
        messages_for_summarization = messages[:-2]
        logger.debug(
            f"Excluding current input message. Processing {len(messages_for_summarization)} historical messages")

        # Determine summarization necessity and execute
        if self.should_summarize(messages_for_summarization, running_summary, is_summarized):
            logger.info("Starting summarization process")

            messages_to_summarize, processed_message_ids = self._get_messages_to_summarize(messages_for_summarization,
                                                                                           running_summary)

            summary = await self.create_summary(messages_to_summarize=messages_to_summarize, llm=llm)
            updated_summary = self.update_running_summary(running_summary, summary, messages_to_summarize,
                                                          processed_message_ids)
            logger.info("Summarization completed")
            return updated_summary
        else:
            logger.debug("No summarization needed")
            return running_summary

    def build_prompt_with_summary(self, messages: List, user_message: str, running_summary: Optional[RunningSummary],
                                  system_prompt: str, exclude_current_input: bool = False) -> List[Dict[str, str]]:
        """Configure prompt considering summary"""
        prompt_messages = [{"role": "system", "content": system_prompt}]

        # Handle exclude current input message option
        messages_for_prompt = messages
        if exclude_current_input and messages:
            # Use only existing messages excluding last message (current user input) for prompt
            messages_for_prompt = messages[:-1]
            logger.debug(
                f"Excluding current input from prompt history. Using {len(messages_for_prompt)} historical messages")

        msg_count = len(messages_for_prompt) if messages_for_prompt else 0

        # If summary exists, configure summary + recent conversation
        if running_summary and running_summary.summary:
            summary_text = running_summary.summary
            summarized_ids = running_summary.summarized_message_ids

            logger.debug(f"Using summary with {len(summarized_ids)} summarized message IDs")

            # Add summary
            prompt_messages.append({
                "role": "system",
                "content": f"Previous conversation summary:\n{summary_text}"
            })

            # Add recent messages that are not summarized (excluding tool messages)
            recent_messages = []
            for msg in messages_for_prompt:
                msg_id = None
                if hasattr(msg, 'id') and msg.id:
                    msg_id = str(msg.id)
                elif hasattr(msg, 'additional_kwargs') and 'id' in msg.additional_kwargs:
                    msg_id = str(msg.additional_kwargs['id'])

                # Add only unsummarized messages
                if msg_id and msg_id not in summarized_ids:
                    recent_messages.append(msg)
                elif not msg_id:  # Messages without ID are considered recent messages
                    recent_messages.append(msg)

            logger.debug(f"Adding {len(recent_messages)} recent messages")

            for msg in recent_messages:
                # Exclude ToolMessage and add only HumanMessage and AIMessage
                if hasattr(msg, 'type') and msg.type in ['human', 'ai']:
                    role = "user" if msg.type == "human" else "assistant"
                    prompt_messages.append({"role": role, "content": msg.content})

        else:
            # If no summary exists, add entire conversation history (excluding tool messages)
            logger.debug(f"No summary available - using all {msg_count} messages")

            if messages_for_prompt:
                for msg in messages_for_prompt:
                    # Exclude ToolMessage and add only HumanMessage and AIMessage
                    if hasattr(msg, 'type') and msg.type in ['human', 'ai']:
                        role = "user" if msg.type == "human" else "assistant"
                        prompt_messages.append({"role": role, "content": msg.content})

        # Add current user message
        if user_message:
            prompt_messages.append({"role": "user", "content": user_message})

        logger.debug(f"Final prompt configuration: {len(prompt_messages)} messages")
        for i, msg in enumerate(prompt_messages):
            role = msg["role"]
            content_preview = str(msg["content"])[:50] + "..." if len(str(msg["content"])) > 50 else str(msg["content"])
            logger.debug(f"   [{i}] {role}: {content_preview}")

        return prompt_messages

    @staticmethod
    def _get_messages_to_summarize(messages: List, running_summary: Optional[RunningSummary]) -> tuple[List, List[str]]:
        """Return messages to summarize and processed message ID list"""
        if running_summary and running_summary.summarized_message_ids:
            # If previous summary exists, summarize only unsummarized messages
            messages_to_summarize = []
            processed_message_ids = []

            for msg in messages:
                msg_id = None
                if hasattr(msg, 'id') and msg.id:
                    msg_id = str(msg.id)
                elif hasattr(msg, 'additional_kwargs') and 'id' in msg.additional_kwargs:
                    msg_id = str(msg.additional_kwargs['id'])

                # Check if message is a processing target (unsummarized message)
                if msg_id and msg_id not in running_summary.summarized_message_ids:
                    processed_message_ids.append(msg_id)  # Add all processed message IDs

                    # Additional check if it's a summarization target (exclude tool/system)
                    if not (hasattr(msg, 'type') and msg.type in ['tool', 'system']):
                        messages_to_summarize.append(msg)
                    else:
                        logger.debug(f"Excluding {msg.type} message from summarization but tracking ID")
                elif not msg_id:  # Messages without ID are considered new messages
                    if not (hasattr(msg, 'type') and msg.type in ['tool', 'system']):
                        messages_to_summarize.append(msg)

            logger.debug(
                f"Found {len(messages_to_summarize)} messages to summarize, {len(processed_message_ids)} total processed IDs")
            return messages_to_summarize, processed_message_ids
        else:
            # For first summarization, process all messages and extract IDs
            messages_to_summarize = []
            processed_message_ids = []

            for msg in messages:
                # Extract message ID
                msg_id = None
                if hasattr(msg, 'id') and msg.id:
                    msg_id = str(msg.id)
                elif hasattr(msg, 'additional_kwargs') and 'id' in msg.additional_kwargs:
                    msg_id = str(msg.additional_kwargs['id'])

                # Add messages with ID to processed ID list
                if msg_id:
                    processed_message_ids.append(msg_id)

                # Check if it's a summarization target (exclude tool/system)
                if not (hasattr(msg, 'type') and msg.type in ['tool', 'system']):
                    messages_to_summarize.append(msg)
                else:
                    logger.debug(f"Excluding {msg.type} message from summarization but tracking ID")

            logger.debug(
                f"First summarization - processing {len(messages_to_summarize)} out of {len(messages)} messages, {len(processed_message_ids)} total processed IDs")
            return messages_to_summarize, processed_message_ids
