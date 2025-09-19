post_alert_analysis_query_description = {
    "api_description": "Submit a query to the alert analysis chat session for intelligent alert investigation.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": {
                            "message_type": "ai",
                            "message": "Based on the alert analysis, I found that the CPU usage spike is related to a memory leak in the application service. Here are the recommended actions: 1) Restart the affected service, 2) Check application logs for memory allocation issues, 3) Monitor memory usage trends over the next 24 hours.",
                            "metadata": {
                                "queries_executed": [
                                    "SELECT * FROM alerts WHERE severity='HIGH' AND timestamp > NOW() - INTERVAL 1 HOUR",
                                    "SELECT avg(cpu_usage) FROM metrics WHERE timestamp > NOW() - INTERVAL 2 HOUR"
                                ],
                                "total_execution_time": 2.3,
                                "tool_calls_count": 3,
                                "databases_accessed": ["mariadb", "influxdb"]
                            }
                        }
                    }
                }
            }
        }
    }
}