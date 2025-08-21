import unittest
from unittest.mock import AsyncMock, patch
import asyncio

# Import the MariaDBServer from the project
from src.server import MariaDBServer

class TestMariaDBServerAsyncio(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.server = MariaDBServer()
        # Patch the _execute_query method to avoid actual DB calls
        self.patcher = patch.object(self.server, '_execute_query', new_callable=AsyncMock)
        self.mock_execute_query = self.patcher.start()

    async def asyncTearDown(self):
        self.patcher.stop()

    async def test_list_databases_returns_list_of_names(self):
        # Simulate DB returning two databases
        self.mock_execute_query.return_value = [
            {'Database': 'information_schema'},
            {'Database': 'chinook'}
        ]
        result = await self.server.list_databases()
        self.assertIsInstance(result, list)
        self.assertIn('information_schema', result)
        self.assertIn('chinook', result)
        self.assertTrue(all(isinstance(db, str) for db in result))

    async def test_list_databases_handles_empty(self):
        self.mock_execute_query.return_value = []
        result = await self.server.list_databases()
        self.assertEqual(result, [])

    async def test_list_databases_handles_exception(self):
        self.mock_execute_query.side_effect = Exception("DB error!")
        with self.assertRaises(Exception):
            await self.server.list_databases()

if __name__ == "__main__":
    unittest.main(verbosity=2)
