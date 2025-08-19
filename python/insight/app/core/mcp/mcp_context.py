# from langgraph.checkpoint.mysql.aio import AIOMySQLSaver
from datetime import datetime, UTC
import aiosqlite
from typing import Any, Coroutine
from langchain_core.messages.utils import count_tokens_approximately
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_mcp_adapters.tools import load_mcp_tools
from langmem.short_term import SummarizationNode, RunningSummary
from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from config.ConfigManager import ConfigManager
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver
from langgraph.graph import MessagesState
# 초기 요약용 프롬프트 템플릿 - 전체 대화 포괄적 요약
from langchain_core.prompts import MessagesPlaceholder, HumanMessagePromptTemplate
from langchain_core.prompts.prompt import PromptTemplate


class State(MessagesState):
    context: dict[str, RunningSummary]


class MCPContext:
    def __init__(self, mcp_client):
        self.config = ConfigManager()

        self.memory = AsyncSqliteSaver(aiosqlite.connect("checkpoints/checkpoints.sqlite", check_same_thread=False))

        # Todo
        # checkpointer MariaDB 사용 가능 여부 검증 필요
        # self._build_db_uri()
        # self.memory = AIOMySQLSaver.from_conn_string(self.uri)

        self.mcp_client = mcp_client
        self.mcp_session = None
        self.llm_client = None

        self.tools = None
        self.agent = None
        self.summarization_node = None

    # Todo
    # def _build_db_uri(self):
    #     db_info = self.config.get_db_config()
    #     self.uri = f'mysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}'

    async def astart(self):
        self.mcp_session = await self.mcp_client.start()
        self.tools = await load_mcp_tools(self.mcp_session)

    async def astop(self):
        await self.mcp_client.stop()

    async def get_agent(self, provider: str, model_name: str, provider_credential: str, use_summarization: bool = True):
        if provider == 'ollama':
            self.llm_client = OllamaClient(provider_credential)
        elif provider == 'openai':
            self.llm_client = OpenAIClient(provider_credential)

        self.llm_client.setup(model=model_name)

        if use_summarization:
            # 요약 노드 설정
            self._setup_summarization_node()

            # Agent에 요약 기능과 함께 바인딩
            self.agent = self.llm_client.bind_tools_with_summarization(
                self.tools,
                self.memory,
                self.summarization_node
            )
        else:
            # 기존 방식으로 Agent 바인딩 (요약 없음)
            self.agent = self.llm_client.bind_tools(self.tools, self.memory)

    async def _build_prompt(self, session_id: str, messages: str) -> list[dict[str, str]]:
        history = await self.get_chat_history(session_id)
        msg_count = 0
        if history:
            channel_values = history.get('channel_values', {})
            msg_count = len(channel_values.get('messages', []))

        current_time = datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")

        # TODO 기능, 상황별 Prompt 관리 기능 추가
        system_prompt_config = self.config.get_system_prompt_config()

        if msg_count == 0:
            system_prompt_first = system_prompt_config.get("system_prompt_first")
            system_prompt = system_prompt_first.format(current_time=current_time)
        else:
            system_prompt_default = system_prompt_config.get("system_prompt_default")
            system_prompt = system_prompt_default.format(current_time=current_time)

        return [
            {"role": "system", "content": f"{system_prompt}"},
            {"role": "user", "content": messages}
        ]

    async def aquery(self, session_id, messages):
        history_tokens = 0
        # 대화 히스토리 확인
        history = await self.get_chat_history(session_id)
        if history:
            channel_values = history.get('channel_values', {})
            existing_messages = channel_values.get('messages', [])
            print(f"📚 [CHAT HISTORY] 기존 메시지 수: {len(existing_messages)}")
            
            # 기존 메시지들의 토큰 수 계산
            if existing_messages:
                history_tokens = count_tokens_approximately(existing_messages)
                print(f"🔢 [TOKEN COUNT] 기존 대화 토큰 수: {history_tokens}")
        else:
            print("📚 [CHAT HISTORY] 새로운 대화 시작")
            
        config = self.create_config(session_id)
        # prompt_sum = await self._get_summary_from_state(config=config)
        # print(prompt_sum)
        # prompt = await self._build_prompt(session_id, messages, prompt_sum)
        prompt = await self._build_prompt(session_id, messages)
        
        # 프롬프트 토큰 수 계산
        prompt_tokens = count_tokens_approximately(prompt)
        print(f"🔢 [TOKEN COUNT] 현재 프롬프트 토큰 수: {prompt_tokens}")
        
        # 설정된 임계값과 비교
        summarization_config = self.config.get_chat_summarization_config()
        max_tokens_before_summary = summarization_config.get('max_tokens_before_summary', 256)
        print(f"⚠️  [THRESHOLD] 요약 임계값: {max_tokens_before_summary}")

        response = await self.agent.ainvoke({'messages': prompt}, config=config)

        
        return response

    @staticmethod
    def create_config(session_id):
        return {'configurable': {'thread_id': f'{session_id}'}}

    async def get_chat_history(self, session_id):
        config = self.create_config(session_id)
        checkpoint = await self.memory.aget(config)
        return checkpoint

    def _setup_summarization_node(self):
        """요약 노드 설정 (로그 분석 최적화)"""
        # config에서 요약 설정 가져오기
        summarization_config = self.config.get_chat_summarization_config()

        # 테스트 파일과 동일한 방식으로 요약 모델 설정
        summarization_model = self.llm_client.llm.bind(max_tokens=256)

        # 테스트 파일의 커스텀 프롬프트 패턴 적용
        custom_initial_prompt = ChatPromptTemplate.from_messages([
            MessagesPlaceholder(variable_name="messages", optional=True),
            HumanMessagePromptTemplate(prompt=PromptTemplate(
                input_variables=[],
                template="Please create a comprehensive summary of ALL the conversation messages above, including names, topics discussed, and key details. Don't omit any important information from the beginning of the conversation."
            ))
        ])

        # SummarizationNode 설정 - llm_input_messages 키로 LLM 전용 메시지 전달 (원본 보존)
        self.summarization_node = SummarizationNode(
            token_counter=count_tokens_approximately,
            model=summarization_model,
            max_tokens=summarization_config.get("max_tokens"),
            max_tokens_before_summary=summarization_config.get("max_tokens_before_summary"),
            max_summary_tokens=256,
            initial_summary_prompt=custom_initial_prompt,
            output_messages_key="llm_input_messages"
        )
