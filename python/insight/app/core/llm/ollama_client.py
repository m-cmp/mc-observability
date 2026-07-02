import os

from langchain.agents import create_agent
from langchain_ollama import ChatOllama

# Ollama's default context window (~4k) is far too small for the multi-agent
# server-error workflow: large trace/log/metric evidence overflows it, the model
# loses the tool results and the "return the structured result" instruction, and
# the agent loops until the graph recursion limit. Give it a roomy, tunable ctx.
DEFAULT_OLLAMA_NUM_CTX = 32768


def _ollama_num_ctx() -> int:
    try:
        return int(os.getenv("OLLAMA_NUM_CTX", str(DEFAULT_OLLAMA_NUM_CTX)))
    except (TypeError, ValueError):
        return DEFAULT_OLLAMA_NUM_CTX


class OllamaClient:
    def __init__(self, base_url=None):
        self.base_url = base_url
        self.model = None
        self.llm = None
        self.agent = None

    def setup(self, model: str):
        self.model = model
        self.llm = ChatOllama(
            base_url=self.base_url, model=self.model, temperature=0, num_ctx=_ollama_num_ctx()
        )

    def setup_graph_llm(self, model: str):
        self.model = model
        self.llm = ChatOllama(
            base_url=self.base_url, model=self.model, temperature=0, num_ctx=_ollama_num_ctx()
        )

    def create_agent_runner(
        self,
        tools,
        checkpointer=None,
        system_prompt: str | None = None,
        response_format=None,
        middleware=None,
    ):
        self.agent = create_agent(
            model=self.llm,
            tools=tools or [],
            system_prompt=system_prompt,
            checkpointer=checkpointer,
            response_format=response_format,
            middleware=middleware or [],
        )
        return self.agent

    def bind_tools(self, tools, memory):
        return self.create_agent_runner(tools=tools, checkpointer=memory)
