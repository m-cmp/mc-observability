# MariaDB Ops Server MCP Tool Tests

This directory contains artifacts related to testing the MariaDB Ops Server MCP (Model Context Protocol) tools.

## Purpose

The primary goal of these tests was to verify the basic functionality and robustness of the read-only operations provided by the MariaDB MCP server tools:

-   `mcp0_list_databases`
-   `mcp0_list_tables`
-   `mcp0_get_table_schema`
-   `mcp0_execute_sql`

## Execution Method

Tests were performed **manually** via the AI Assistant interface. The interface invoked the MCP tools directly based on user requests, and the results (or errors) were observed in the assistant's responses.

From there, tests were converted into code in test_mariadb_mcp_tools.py`. We use the python `unittest` framework to structure the tests. Note that the environment variables are still used in the unit tests, and a live mariadb server is required to run the tests currently.

## Test Cases

The specific test cases executed are documented in the `test_mariadb_mcp_tools.py` script within this directory. This script serves as a record of the manual tests performed and includes:

1.  **Basic Functionality Tests:** Verifying core operations like listing databases/tables, getting schema, and executing simple SELECTs.
2.  **Complex/Edge Case Tests:** Checking behavior with:
    *   Non-existent databases/tables
    *   Complex SQL (JOINs, Aggregations)
    *   Parameterized queries with edge-case values (empty strings)
    *   Parameter count mismatches
    *   `SHOW` commands (including necessary wildcard escaping)

## Summary of Results

All tests executed as expected. The tools successfully performed the requested read-only operations and provided appropriate error messages for invalid inputs or non-existent objects. The `mcp0_execute_sql` tool required correct escaping (`%%`) for literal `%` signs in `LIKE` clauses when used with `SHOW` commands.

## `test_mariadb_mcp_tools.py`

This Python script outlines the tests performed. It is **not** an automated test suite but rather a structured documentation of the manual steps and observed outcomes. It cannot be run independently to interact with the MCP tools.
