from langchain_ollama import ChatOllama
from langchain_openai import ChatOpenAI

from langgraph.prebuilt import create_react_agent

# import aiosqlite

class OllamaClient:
    def __init__(self, model="llama3.1"):
        self.model = model
        self.base_url = "http://192.168.170.229:11434"
        self.llm = ChatOllama(base_url=self.base_url, model=self.model)
        # self.llm = ChatOpenAI(model='gpt-4o-mini', temperature=0)
        self.agent = None



    def generate(self, prompt: str) -> str:
        print(f'prompt type: {type(prompt)}')
        response = self.llm.invoke({'messages': prompt})
        return response


    def setup(self, tools):
        self.agent = create_react_agent(model=self.llm, tools=tools)

        return self.agent