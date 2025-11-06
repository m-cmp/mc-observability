post_log_analysis_query_description = {
    "api_description": "Submit a query to the log analysis chat session for intelligent log investigation and troubleshooting.",
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
                            "message": "I've analyzed the error logs from the past hour and identified 3 critical "
                            "issues: 1) Database connection timeout errors (15 occurrences), 2) "
                            "Authentication service failures (8 occurrences), 3) Memory allocation errors "
                            "(3 occurrences). The root cause appears to be network connectivity issues "
                            "between the application and database servers.",
                        },
                    }
                }
            },
        }
    },
}
