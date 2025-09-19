get_llm_sessions_description = {
    "api_description": "Retrieve all active LLM chat sessions for log analysis.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": [
                            {
                                "seq": 1,
                                "user_id": "1",
                                "session_id": "986ef65f-e425-478a-b84c-03fa80682f36",
                                "provider": "openai",
                                "model_name": "gpt-5-mini",
                                "regdate": "2025-09-17T14:12:36"
                            },
                            {
                                "seq": 2,
                                "user_id": "1",
                                "session_id": "40dadcfa-98d6-4d5b-9745-669928c496b7",
                                "provider": "ollama",
                                "model_name": "llama3.1:8b",
                                "regdate": "2025-09-18T11:46:49"
                            }
                        ]
                    }
                }
            }
        }
    }
}

post_llm_session_description = {
    "api_description": "Create a new LLM chat session for log analysis with specified provider and model.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": {
                            "seq": 3,
                            "user_id": "1",
                            "session_id": "5e0f421b-7f88-4c1d-9132-f468ec9c557b",
                            "provider": "openai",
                            "model_name": "gpt-5-mini",
                            "regdate": "2025-09-18T14:54:04"
                        }
                    }
                }
            }
        }
    }
}

delete_llm_session_description = {
    "api_description": "Delete a specific LLM chat session and its conversation history.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": {
                            "seq": 4,
                            "user_id": "1",
                            "session_id": "5e0f421b-7f88-4c1d-9132-f468ec9c557b",
                            "provider": "openai",
                            "model_name": "gpt-5-mini",
                            "regdate": "2025-09-18T14:54:04"
                        }
                    }
                }
            }
        }
    }
}

delete_all_llm_sessions_description = {
    "api_description": "Delete all LLM chat sessions and their conversation histories.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": []
                    }
                }
            }
        }
    }
}

get_llm_session_history_description = {
    "api_description": "Retrieve the conversation history for a specific LLM chat session.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": {
                            "messages": [
                                {
                                    "message_type": "human",
                                    "message": "Analyze these error logs and find the root cause",
                                },
                                {
                                    "message_type": "ai",
                                    "message": "I’ll find the actual error logs (and some context)...",
                                }
                            ],
                            "seq": 41,
                            "user_id": "1",
                            "session_id": "fa746359-ab1b-4755-a1cc-630e373135ff",
                            "provider": "openai",
                            "model_name": "gpt-5-mini",
                            "regdate": "2025-09-18T14:59:37"
                        }
                    }
                }
            }
        }
    }
}
