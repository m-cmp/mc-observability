import logging
from typing import List, Dict, Optional
from langchain_core.messages.utils import count_tokens_approximately
from langmem.short_term import RunningSummary


logger = logging.getLogger(__name__)


class ConversationSummarizer:
    """대화 요약 관리 클래스"""
    
    def __init__(self, config_manager):
        self.config = config_manager
    
    def should_summarize(self, messages: List, running_summary: Optional[RunningSummary] = None) -> bool:
        """토큰 수 기반으로 요약 필요 여부 판단"""
        summarization_config = self.config.get_chat_summarization_config()
        max_tokens = summarization_config.get('max_tokens_before_summary', 256)

        history_tokens = 0
        if messages:
            history_tokens = count_tokens_approximately(messages)

        should_summarize = history_tokens > max_tokens
        
        logger.debug(f"Token count: {history_tokens}, threshold: {max_tokens}, should_summarize: {should_summarize}")
        
        # 첫 대화는 요약하지 않음
        if not running_summary or not running_summary.summary:
            if history_tokens == 0:
                logger.debug("First conversation, skipping summarization")
                return False
        
        return should_summarize
    
    async def create_summary(self, messages_to_summarize: List, llm) -> str:
        """지정된 메시지들을 요약"""
        logger.info(f"Starting to summarize {len(messages_to_summarize)} messages")
        
        # 요약할 메시지가 없으면 빈 문자열 반환
        if not messages_to_summarize:
            return ""
        
        # 요약용 프롬프트 구성
        summary_prompt = """
        다음 대화 메시지들을 간결하게 요약해주세요. 주요 주제, 사용자 요청, AI 응답의 핵심 내용을 포함해주세요:
        {messages}
        요약:
        """
        
        # 메시지들을 텍스트로 변환
        messages_text = ""
        for i, msg in enumerate(messages_to_summarize):
            if hasattr(msg, 'content'):
                content = msg.content
            elif isinstance(msg, dict):
                content = msg.get('content', str(msg))
            else:
                content = str(msg)
            messages_text += f"[{i+1}] {content}\n"
        
        formatted_prompt = summary_prompt.format(messages=messages_text)
        
        try:
            # LLM을 직접 사용해서 요약 생성
            summary_response = await llm.ainvoke([{"role": "user", "content": formatted_prompt}])
            summary = summary_response.content.strip()
            
            logger.info(f"Summary created successfully (length: {len(summary)} characters)")
            logger.debug(f"Summary preview: {summary[:100]}...")
            
            return summary
            
        except Exception as e:
            logger.error(f"Summary creation failed: {type(e).__name__}: {e}")
            return ""
    
    @staticmethod
    def update_running_summary(current_summary: Optional[RunningSummary], new_summary: str, messages_to_summarize: List) -> RunningSummary:
        """RunningSummary 객체 업데이트"""
        # 메시지 ID 추출
        summarized_ids = set()
        last_message_id = None
        
        for msg in messages_to_summarize:
            if hasattr(msg, 'id') and msg.id:
                msg_id = str(msg.id)
                summarized_ids.add(msg_id)
                last_message_id = msg_id
            elif hasattr(msg, 'additional_kwargs') and 'id' in msg.additional_kwargs:
                msg_id = str(msg.additional_kwargs['id'])
                summarized_ids.add(msg_id)
                last_message_id = msg_id
        
        if current_summary and current_summary.summary:
            # 기존 요약과 합치기
            combined_summary = f"{current_summary.summary}\n\n[이전 대화 계속]\n{new_summary}"
            combined_ids = current_summary.summarized_message_ids.union(summarized_ids)
        else:
            # 첫 요약
            combined_summary = new_summary
            combined_ids = summarized_ids
        
        logger.debug("Updating running summary state")
        logger.debug(f"Summary length: {len(combined_summary)} characters")
        logger.debug(f"Summarized message ID count: {len(combined_ids)}")
        logger.debug(f"Last summarized message ID: {last_message_id}")
        
        return RunningSummary(
            summary=combined_summary,
            summarized_message_ids=combined_ids,
            last_summarized_message_id=last_message_id
        )
    
    async def process(self, messages: List, llm, running_summary: Optional[RunningSummary] = None) -> Optional[RunningSummary]:
        """요약 처리 실행 - RunningSummary 객체 반환"""
        logger.debug(f"Message count: {len(messages)}")
        
        # 요약 필요성 판단 및 실행
        if self.should_summarize(messages, running_summary):
            logger.info("Starting summarization process")
            
            # 요약할 메시지 범위 결정
            messages_to_summarize = self._get_messages_to_summarize(messages, running_summary)
            
            if messages_to_summarize:
                summary = await self.create_summary(messages_to_summarize=messages_to_summarize, llm=llm)
                if summary:
                    updated_summary = self.update_running_summary(running_summary, summary, messages_to_summarize)
                    logger.info("Summarization completed")
                    return updated_summary
                else:
                    logger.error("Summarization failed")
                    return running_summary
            else:
                logger.debug("No messages to summarize")
                return running_summary
        else:
            logger.debug("No summarization needed")
            return running_summary
    
    def build_prompt_with_summary(self, messages: List, user_message: str, running_summary: Optional[RunningSummary], system_prompt: str) -> List[Dict[str, str]]:
        """요약을 고려한 프롬프트 구성"""
        prompt_messages = [{"role": "system", "content": system_prompt}]
        msg_count = len(messages) if messages else 0
        
        # 요약이 있는 경우 요약 + 최근 대화 구성
        if running_summary and running_summary.summary:
            summary_text = running_summary.summary
            summarized_ids = running_summary.summarized_message_ids
            
            logger.debug(f"Using summary with {len(summarized_ids)} summarized message IDs")
            
            # 요약 추가
            prompt_messages.append({
                "role": "system", 
                "content": f"이전 대화 요약:\n{summary_text}"
            })
            
            # 요약되지 않은 최근 메시지들 추가 (tool messages 제외)
            recent_messages = []
            for msg in messages:
                msg_id = None
                if hasattr(msg, 'id') and msg.id:
                    msg_id = str(msg.id)
                elif hasattr(msg, 'additional_kwargs') and 'id' in msg.additional_kwargs:
                    msg_id = str(msg.additional_kwargs['id'])
                
                # 요약되지 않은 메시지만 추가
                if msg_id and msg_id not in summarized_ids:
                    recent_messages.append(msg)
                elif not msg_id:  # ID가 없는 메시지는 최근 메시지로 간주
                    recent_messages.append(msg)
            
            logger.debug(f"Adding {len(recent_messages)} recent messages")
            
            for msg in recent_messages:
                # ToolMessage는 제외하고 HumanMessage와 AIMessage만 추가
                if hasattr(msg, 'type') and msg.type in ['human', 'ai']:
                    role = "user" if msg.type == "human" else "assistant"
                    prompt_messages.append({"role": role, "content": msg.content})
        
        else:
            # 요약이 없는 경우 전체 대화 이력 추가 (tool messages 제외)
            logger.debug(f"No summary available - using all {msg_count} messages")
            
            if messages:
                for msg in messages:
                    # ToolMessage는 제외하고 HumanMessage와 AIMessage만 추가
                    if hasattr(msg, 'type') and msg.type in ['human', 'ai']:
                        role = "user" if msg.type == "human" else "assistant"
                        prompt_messages.append({"role": role, "content": msg.content})
        
        # 현재 사용자 메시지 추가
        if user_message:
            prompt_messages.append({"role": "user", "content": user_message})
        
        logger.debug(f"Final prompt configuration: {len(prompt_messages)} messages")
        for i, msg in enumerate(prompt_messages):
            role = msg["role"]
            content_preview = str(msg["content"])[:50] + "..." if len(str(msg["content"])) > 50 else str(msg["content"])
            logger.debug(f"   [{i}] {role}: {content_preview}")
        
        return prompt_messages

    def _get_messages_to_summarize(self, messages: List, running_summary: Optional[RunningSummary]) -> List:
        """요약할 메시지 범위 결정"""
        if not messages:
            return []
        
        if running_summary and running_summary.summarized_message_ids:
            # 이전 요약이 있는 경우, 요약되지 않은 메시지들만 요약
            messages_to_summarize = []
            for msg in messages:
                msg_id = None
                if hasattr(msg, 'id') and msg.id:
                    msg_id = str(msg.id)
                elif hasattr(msg, 'additional_kwargs') and 'id' in msg.additional_kwargs:
                    msg_id = str(msg.additional_kwargs['id'])
                
                # 요약되지 않은 메시지만 추가
                if msg_id and msg_id not in running_summary.summarized_message_ids:
                    messages_to_summarize.append(msg)
                elif not msg_id:  # ID가 없는 메시지는 새 메시지로 간주
                    messages_to_summarize.append(msg)
            
            logger.debug(f"Found {len(messages_to_summarize)} unsummarized messages")
            return messages_to_summarize
        else:
            # 첫 요약인 경우, 모든 메시지 요약
            logger.debug(f"First summarization - processing all {len(messages)} messages")
            return messages