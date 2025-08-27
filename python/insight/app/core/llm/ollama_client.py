from langchain_ollama import ChatOllama


class OllamaClient:
    def __init__(self, base_url='http://192.168.170.229:11434'):
        self.base_url = base_url
        self.model = None
        self.llm = None
        self.agent = None

    def setup(self, model='llama3.1:8b'):
        self.model = model
        self.llm = ChatOllama(base_url=self.base_url, model=self.model, temperature=0)

    # def bind_tools(self, tools, memory):
    #     self.agent = create_react_agent(model=self.llm, tools=tools, checkpointer=memory)
    #     return self.agent
