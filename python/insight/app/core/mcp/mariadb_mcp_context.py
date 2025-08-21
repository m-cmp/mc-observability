from mcp import ClientSession
from mcp.client.sse import sse_client


class MariaDBMCPContext:
    def __init__(self, mcp_url):
        self.mcp_url = mcp_url
        self._sse = None
        self.session = None
        self.tools = None
        self._read = None
        self._write = None

    async def astart(self):
        print(f"[DEBUG] Starting MariaDB MCP connection to: {self.mcp_url}")
        try:
            self._sse = sse_client(self.mcp_url)
            self._read, self._write = await self._sse.__aenter__()
            print("[DEBUG] SSE connection established")

            self.session = ClientSession(self._read, self._write)
            await self.session.__aenter__()
            print("[DEBUG] MCP session created")

            await self.session.initialize()
            print("[DEBUG] MCP session initialized")

            # 도구 목록 로드
            self.tools = await self.session.list_tools()
            print(f"Successfully loaded {len(self.tools.tools)} MariaDB MCP tools")
            return self.session

        except Exception as e:
            print(f"Failed to start MariaDB MCP context: {e}")
            import traceback

            traceback.print_exc()
            await self.astop()
            raise

    async def astop(self):
        try:
            if self.session:
                await self.session.__aexit__(None, None, None)
        except Exception as e:
            print(f"Error closing MariaDB MCP session: {e}")

        try:
            if self._sse:
                await self._sse.__aexit__(None, None, None)
        except Exception as e:
            print(f"Error closing MariaDB MCP SSE: {e}")

    async def get_tools(self):
        if self.tools is None:
            self.tools = await self.session.list_tools()
        return self.tools

    async def call_tool(self, tool_name, arguments=None):
        if arguments is None:
            arguments = {}

        result = await self.session.call_tool(tool_name, arguments)
        return result
