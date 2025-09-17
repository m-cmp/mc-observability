import logging
import json
import re
import time

# from langgraph.checkpoint.mysql.aio import AIOMySQLSaver
from datetime import UTC, datetime

import aiosqlite
from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from app.core.graph import create_conversation_graph
from app.core.prompts.prompt_factory import PromptFactory
from config.ConfigManager import ConfigManager
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver

logger = logging.getLogger(__name__)


class MCPContext:
    def __init__(self, mcp_manager, analysis_type: str):
        self.config = ConfigManager()
        self.memory = AsyncSqliteSaver(aiosqlite.connect("checkpoints/checkpoints.sqlite", check_same_thread=False))
        self.mcp_manager = mcp_manager
        self.analysis_type = analysis_type
        self.prompt_service = PromptFactory.create_prompt_service(analysis_type, self.config)
        self.llm_client = None
        self.llm_with_tools = None
        self.tools = None
        self.agent = None
        self.query_metadata = {"queries_executed": [], "total_execution_time": 0.0, "tool_calls_count": 0, "databases_accessed": set()}

    # Todo
    # def _build_db_uri(self):
    #     db_info = self.config.get_db_config()
    #     self.uri = f'mysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}'

    def reset_metadata(self):
        """Initialize metadata when starting a new query"""
        self.query_metadata = {"queries_executed": [], "total_execution_time": 0.0, "tool_calls_count": 0, "databases_accessed": set()}

    def track_query_execution(self, query: str, execution_time: float, database: str = None):
        """Track query execution information"""
        self.query_metadata["queries_executed"].append(query)
        self.query_metadata["total_execution_time"] += execution_time
        self.query_metadata["tool_calls_count"] += 1

        if database:
            self.query_metadata["databases_accessed"].add(database)

    def get_metadata_summary(self):
        """Return metadata collected so far"""
        return {
            "queries_executed": self.query_metadata["queries_executed"],
            "total_execution_time": round(self.query_metadata["total_execution_time"], 3),
            "tool_calls_count": self.query_metadata["tool_calls_count"],
            "databases_accessed": list(self.query_metadata["databases_accessed"]),
        }

    async def get_agent(self, provider: str, model_name: str, provider_credential: str, streaming: bool = False):
        try:
            if not provider_credential:
                msg = f"Missing credential for provider '{provider}'."
                logger.error(msg)
                raise ValueError(msg)

            if provider == "ollama":
                self.llm_client = OllamaClient(provider_credential)
            elif provider == "openai":
                self.llm_client = OpenAIClient(provider_credential)
            elif provider == "google":
                self.llm_client = OpenAIClient(provider_credential, base_url="https://generativelanguage.googleapis.com/v1beta/openai")
            elif provider == "anthropic":
                self.llm_client = OpenAIClient(provider_credential, base_url="https://api.anthropic.com/v1/")
            else:
                msg = f"Unsupported provider: {provider}"
                logger.error(msg)
                raise ValueError(msg)
            self.tools = self.mcp_manager.get_all_tools() or []
            logger.info(f"Using {len(self.tools)} tools from multi-MCP environment")

            if self.analysis_type == "log":
                self.llm_client.setup_graph_llm(model=model_name)
                self.llm_with_tools = self.llm_client.llm.bind_tools(tools=self.tools)
                self.agent = await create_conversation_graph(llm=self.llm_with_tools, tools=self.tools, config=self.config)
            else:
                # Pass streaming parameter only to OpenAI-based clients
                if hasattr(self.llm_client, 'setup'):
                    if provider in ["openai", "google", "anthropic"]:
                        self.llm_client.setup(model=model_name, streaming=streaming)
                    else:
                        self.llm_client.setup(model=model_name)
                self.agent = self.llm_client.bind_tools(self.tools, self.memory)
        except Exception as e:
            logger.error(f"Failed to initialize agent for provider={provider}, model={model_name}: {e}")
            raise

    async def _build_prompt(self, session_id: str, user_message: str):
        """Build prompt using the appropriate prompt service based on analysis type."""
        history = await self.get_chat_history(session_id)
        msg_count = 0
        if history:
            channel_values = history.get("channel_values", {})
            msg_count = len(channel_values.get("messages", []))

        return self.prompt_service.build_prompt(session_id, user_message, msg_count)

    async def aquery(self, session_id: str, messages: str):
        self.reset_metadata()
        start_time = time.time()

        config = self.create_config(session_id)
        prompt = await self._build_prompt(session_id, messages)

        response = await self.agent.ainvoke({"messages": prompt}, config=config)

        total_time = time.time() - start_time
        self.query_metadata["total_execution_time"] = round(total_time, 3)

        self._extract_tool_calls_from_response(response)

        return response

    # async def astream_query(self, session_id, messages):
    #     """Stream tokens as SSE while tracking metadata.
    #     Yields bytes formatted as Server-Sent Events (text/event-stream):
    #     - token events:  data: {"delta": "..."}\n\n
    #     - end event:    event: end\n data: {"metadata": {...}}\n\n
    #     """
    #     self.reset_metadata()
    #     start_time = time.time()
    #
    #     config = self.create_config(session_id)
    #     prompt = await self._build_prompt(session_id, messages)
    #
    #     yield b":ok\n\n"
    #
    #     try:
    #         async for event in self.agent.astream_events({"messages": prompt}, config=config, version="v1"):
    #             et = event.get("event", "")
    #             data = event.get("data", {})
    #
    #             if et in ("on_chat_model_stream", "on_llm_stream"):
    #                 chunk = data.get("chunk")
    #                 text = None
    #                 try:
    #                     if hasattr(chunk, "content"):
    #                         c = getattr(chunk, "content")
    #                         if isinstance(c, str):
    #                             text = c
    #                         elif isinstance(c, list):
    #                             parts = []
    #                             for p in c:
    #                                 if isinstance(p, dict) and isinstance(p.get("text"), str):
    #                                     parts.append(p["text"])
    #                                 elif hasattr(p, "text") and isinstance(getattr(p, "text"), str):
    #                                     parts.append(getattr(p, "text"))
    #                             if parts:
    #                                 text = "".join(parts)
    #                     elif hasattr(chunk, "text") and isinstance(getattr(chunk, "text"), str):
    #                         text = getattr(chunk, "text")
    #                 except Exception:
    #                     text = None
    #
    #                 if text:
    #                     payload = json.dumps({"delta": text}, ensure_ascii=False)
    #                     yield (f"data: {payload}\n\n").encode("utf-8")
    #
    #             elif et == "on_chain_end":
    #                 try:
    #                     output = data.get("output")
    #                     if output:
    #                         self._extract_tool_calls_from_response(output)
    #                 except Exception:
    #                     pass
    #
    #         total_time = time.time() - start_time
    #         self.query_metadata["total_execution_time"] = round(total_time, 3)
    #
    #         meta = self.get_metadata_summary()
    #         yield (f"event: end\ndata: {json.dumps({'metadata': meta}, ensure_ascii=False)}\n\n").encode("utf-8")
    #
    #     except Exception as e:
    #         logger.error(f"Streaming error: {e}")
    #         err = json.dumps({"error": str(e)})
    #         yield (f"event: error\ndata: {err}\n\n").encode("utf-8")

    def _extract_tool_calls_from_response(self, response):
        """Extract tool call information from LangGraph response.
        Policy change: queries_executed includes only pure query strings.
        - Regex extraction from message.content is disabled
        - Collect only from query/influxql_query/sql_query keys in tool_calls.args
        - Remove leading/trailing whitespace and duplicates
        """
        try:
            messages = response.get("messages", [])
            logger.debug(f"Extracting from {len(messages)} messages")

            for message in messages:
                logger.debug(f"Message type: {getattr(message, 'type', 'unknown')}")

                if hasattr(message, "type") and message.type == "tool":
                    self.query_metadata["tool_calls_count"] += 1
                    logger.debug(f"Found tool call, count: {self.query_metadata['tool_calls_count']}")

                elif hasattr(message, "type") and message.type == "ai":
                    if hasattr(message, "tool_calls") and message.tool_calls:
                        tool_calls = message.tool_calls
                        try:
                            tc_len = len(tool_calls)
                        except Exception:
                            tc_len = 0
                        self.query_metadata["tool_calls_count"] += tc_len
                        logger.debug(f"Found {tc_len} tool calls in AI message")

                        for tool_call in tool_calls:
                            call_name = None
                            if isinstance(tool_call, dict):
                                call_name = tool_call.get("name")
                                if not call_name:
                                    fn = tool_call.get("function")
                                    if isinstance(fn, dict):
                                        call_name = fn.get("name")
                            elif hasattr(tool_call, "name"):
                                call_name = tool_call.name

                            inferred_db = None
                            if isinstance(call_name, str):
                                name_lower = call_name.lower()
                                influx_tools = {
                                    "execute_influxql",
                                    "get_time_window_summary",
                                    "get_last_data_point_timestamp",
                                    "get_measurement_schema",
                                    "get_tag_values",
                                    "list_measurements",
                                    "list_influxdb_databases",
                                }
                                maria_tools = {
                                    "execute_sql",
                                    "query_sql",
                                    "list_databases",
                                    "list_tables",
                                }
                                grafana_tools = {
                                    "query_loki_logs",
                                    "list_loki_label_names",
                                    "list_loki_label_values",
                                    "query_loki_stats",
                                    "list_datasources",
                                    "get_datasource_by_uid",
                                    "get_datasource_by_name",
                                    "generate_deeplink",
                                }
                                if ("influx" in name_lower) or (call_name in influx_tools):
                                    inferred_db = "InfluxDB"
                                elif ("maria" in name_lower) or ("mysql" in name_lower) or (call_name in maria_tools):
                                    inferred_db = "MariaDB"
                                elif ("grafana" in name_lower) or (call_name in grafana_tools):
                                    inferred_db = "Grafana"

                            args_dict = None
                            if isinstance(tool_call, dict):
                                args_dict = tool_call.get("args") or tool_call.get("arguments")
                                if args_dict is None:
                                    fn = tool_call.get("function")
                                    if isinstance(fn, dict):
                                        args_dict = fn.get("arguments")
                            elif hasattr(tool_call, "args"):
                                args_val = getattr(tool_call, "args")
                                if args_val is not None:
                                    args_dict = args_val

                            if isinstance(args_dict, str):
                                try:
                                    import json as _json

                                    parsed = _json.loads(args_dict)
                                    if isinstance(parsed, dict):
                                        args_dict = parsed
                                except Exception:
                                    args_dict = None

                            if isinstance(args_dict, dict):
                                captured_any_query_for_call = False
                                for key, value in args_dict.items():
                                    if key and isinstance(key, str) and key.lower() in ("query", "influxql_query", "sql_query") and isinstance(value, str):
                                        q = value.strip()
                                        q = re.sub(r"\s+", " ", q)
                                        if q and q not in self.query_metadata["queries_executed"]:
                                            self.query_metadata["queries_executed"].append(q)
                                            captured_any_query_for_call = True
                                            logger.debug(f"Captured query: {q[:160]}...")

                                if captured_any_query_for_call and inferred_db:
                                    self.query_metadata["databases_accessed"].add(inferred_db)

            logger.debug(f"Final metadata: {self.query_metadata}")

        except Exception as e:
            logger.error(f"Error extracting tool calls: {e}")
            import traceback

            traceback.print_exc()

    @staticmethod
    def create_config(session_id):
        return {"configurable": {"thread_id": f"{session_id}", "checkpoint_ns": ""}}

    async def get_chat_history(self, session_id):
        config = self.create_config(session_id)
        checkpoint = await self.memory.aget(config)
        return checkpoint
