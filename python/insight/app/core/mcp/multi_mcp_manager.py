from app.core.mcp.mcp_context import MCPContext
from app.core.mcp.mariadb_mcp_context import MariaDBMCPContext
from app.core.mcp.influxdb_mcp_context import InfluxDBMCPContext
from typing import Dict, List
from langchain_mcp_adapters.tools import load_mcp_tools


class MCPManager:
    def __init__(self):
        self.mcp_clients: Dict[str, object] = {}
        self.mcp_contexts: Dict[str, MCPContext] = {}
        self.all_tools: List = []

    def add_mariadb_mcp(self, name: str, mcp_url: str):
        """MariaDB MCP 클라이언트를 추가합니다."""
        client = MariaDBMCPContext(mcp_url)
        self.mcp_clients[name] = client
        return client

    def add_influxdb_mcp(self, name: str, mcp_url: str):
        """InfluxDB MCP 클라이언트를 추가합니다."""
        client = InfluxDBMCPContext(mcp_url)
        self.mcp_clients[name] = client
        return client

    async def start_all(self):
        """모든 MCP 클라이언트를 시작합니다."""
        sessions = {}
        tools_list = []

        for name, client in self.mcp_clients.items():
            try:
                print(f"[DEBUG] Starting {name} MCP client...")
                session = await client.astart()
                sessions[name] = session

                # 각 MCP의 도구들을 로드
                tools = await load_mcp_tools(session)
                tools_list.extend(tools)
                print(f"[DEBUG] {name} MCP client started successfully with {len(tools)} tools")

            except Exception as e:
                print(f"Failed to start {name} MCP client: {e}")
                import traceback

                traceback.print_exc()
                # 실패한 클라이언트는 제거하지 않고 계속 진행
                continue

        self.all_tools = tools_list
        print(f"[DEBUG] Total MCP tools loaded: {len(self.all_tools)}")
        return sessions

    async def stop_all(self):
        """모든 MCP 클라이언트를 중지합니다."""
        for name, client in self.mcp_clients.items():
            try:
                await client.astop()
                print(f"{name} MCP client stopped successfully")
            except Exception as e:
                print(f"Failed to stop {name} MCP client: {e}")
                # 오류가 발생해도 다른 클라이언트들은 계속 처리

    def get_all_tools(self):
        """모든 MCP 클라이언트의 도구를 반환합니다."""
        return self.all_tools

    def get_client(self, name: str):
        """특정 이름의 클라이언트를 반환합니다."""
        return self.mcp_clients.get(name)

    def get_client_by_tool(self, tool_name: str):
        """특정 도구를 가진 클라이언트를 찾아 반환합니다."""
        for name, client in self.mcp_clients.items():
            if hasattr(client, "tools") and client.tools:
                for tool in client.tools.tools:
                    if tool.name == tool_name:
                        return client
        return None
