import aiosqlite
from langgraph.graph import StateGraph, START, END
from langgraph.prebuilt import ToolNode
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver
from .edges import should_continue
from .nodes import call_model, summary_node
from .state import State


class GraphBuilder:
    """Builder class for constructing and configuring the conversation graph."""

    def __init__(self, llm=None, tools=None, config=None):
        """
        Initialize the graph builder with dependencies.
        
        Args:
            llm_client: LLM client instance (OpenAI/Ollama)
            tools: List of available tools
            config: Configuration manager instance
        """
        self.llm = llm
        self.tools = tools or []
        self.config = config
        self.graph_builder = None
        self.checkpointer = None

    async def initialize(self, checkpoint_path: str = "checkpoints/checkpoints.sqlite"):
        """
        Initialize the graph builder with checkpointing.
        
        Args:
            checkpoint_path: Path to SQLite checkpoint database
        """
        conn = await aiosqlite.connect(checkpoint_path, check_same_thread=False)
        self.checkpointer = AsyncSqliteSaver(conn)
        self.graph_builder = StateGraph(State)

    def _create_call_model_node(self):
        """Create call_model node with injected dependencies."""

        async def call_model_with_deps(state: State):
            return await call_model(state, llm=self.llm)

        return call_model_with_deps

    def _create_tool_node(self):
        """Create tool node with injected dependencies."""

        async def tool_node_with_deps(state: State):
            messages = (state["llm_input_messages"] if state.get("is_summarized", False) else state["messages"])

            tool_node = ToolNode(self.tools)
            result = await tool_node.ainvoke({"messages": messages})

            if state.get("is_summarized", False):
                # Only add response to existing summary messages in summarized mode
                updated_llm_input_messages = state["llm_input_messages"] + result.get("messages", [])
                result["llm_input_messages"] = updated_llm_input_messages

            return result

        return tool_node_with_deps

    def _create_summary_node(self):
        """Create summary node with injected dependencies."""

        async def summary_node_with_deps(state: State):
            return await summary_node(state, llm=self.llm, config_manager=self.config)

        return summary_node_with_deps

    def add_nodes(self):
        """Add all nodes to the graph."""
        self.graph_builder.add_node("summary", self._create_summary_node())
        self.graph_builder.add_node("call_model", self._create_call_model_node())
        self.graph_builder.add_node("tools", self._create_tool_node())

    def add_edges(self):
        """Add edges and conditional edges to the graph."""
        # START → summary → call_model
        self.graph_builder.add_edge(START, "summary")
        self.graph_builder.add_edge("summary", "call_model")

        # call_model → should_continue → tools/END
        self.graph_builder.add_conditional_edges(
            "call_model",
            should_continue,
            {
                "continue": "tools",
                "end": END,
            }
        )

        # tools → call_model (loop back)
        self.graph_builder.add_edge("tools", "call_model")

    async def compile(self):
        """
        Compile the graph with checkpointing enabled.
        
        Returns:
            Compiled graph ready for execution
        """
        return self.graph_builder.compile(checkpointer=self.checkpointer)

    async def build_graph(self, checkpoint_path: str = "checkpoints/checkpoints.sqlite"):
        """
        Build complete conversation graph with all nodes and edges.
        
        Args:
            checkpoint_path: Path to SQLite checkpoint database
            
        Returns:
            Compiled graph ready for execution
        """
        await self.initialize(checkpoint_path)
        self.add_nodes()
        self.add_edges()
        return await self.compile()


async def create_conversation_graph(llm, tools, config, checkpoint_path: str = "checkpoints/checkpoints.sqlite"):
    """
    Convenience function to create a complete conversation graph.
    
    Args:
        llm: LLM client instance (OpenAI/Ollama/etc)
        tools: List of available tools
        config: Configuration manager instance
        checkpoint_path: Path to SQLite checkpoint database
        
    Returns:
        Compiled graph ready for execution
    """
    builder = GraphBuilder(llm=llm, tools=tools, config=config)
    return await builder.build_graph(checkpoint_path)
