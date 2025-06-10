from langchain_ollama import ChatOllama

from langgraph.prebuilt import create_react_agent
from langgraph.checkpoint.memory import MemorySaver


class OllamaClient:
    def __init__(self, model="llama3.1"):
        self.model = model
        self.base_url = "http://192.168.170.229:11434"
        self.llm = ChatOllama(base_url=self.base_url, model=self.model)
        self.agent = None
        self.memory = MemorySaver()


    def generate(self, prompt: str) -> str:
        print(f'prompt type: {type(prompt)}')
        response = self.llm.invoke({'messages': prompt})
        return response


    async def setup(self, tools):
        self.agent = create_react_agent(self.llm, tools=tools, checkpointer=self.memory)

        return self.agent, self.memory