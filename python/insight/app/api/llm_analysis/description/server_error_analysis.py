post_server_error_detect_description = {
    "api_description": "Detect recent HTTP 5xx server errors and run analysis.",
    "response": {200: {"description": "Accepted server error analysis requests"}},
}

post_server_error_query_description = {
    "api_description": "Run manual HTTP 5xx analysis using the same graph and MCP tools as automatic analysis.",
    "response": {200: {"description": "Server error analysis response"}},
}

get_server_error_records_description = {
    "api_description": "List HTTP 5xx analysis records.",
    "response": {200: {"description": "Server error analysis records"}},
}

get_server_error_record_detail_description = {
    "api_description": "Get HTTP 5xx analysis detail by analysis ID.",
    "response": {200: {"description": "Server error analysis record"}},
}

post_server_error_rerun_description = {
    "api_description": "Reset an HTTP 5xx analysis record and run analysis again.",
    "response": {200: {"description": "Server error analysis rerun response"}},
}
