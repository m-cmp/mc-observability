from langchain.agents import create_agent
from langchain_openai import ChatOpenAI


class OpenAIClient:
    def __init__(self, api_key: str, base_url=None):
        self.model = None
        self.api_key = api_key
        self.base_url = base_url
        self.llm = None
        self.agent = None

    def setup(self, model, streaming=False):
        self.model = model
        if self.base_url:
            self.llm = ChatOpenAI(model=self.model, api_key=self.api_key, base_url=self.base_url, streaming=streaming)
        else:
            self.llm = ChatOpenAI(model=self.model, api_key=self.api_key, streaming=streaming)

    def setup_graph_llm(self, model, streaming=False):
        self.model = model
        self.llm = ChatOpenAI(model=self.model, api_key=self.api_key, base_url=self.base_url, streaming=streaming)

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
