get_llm_model_options_description = {
    "api_description": "Retrieve available LLM model options and configurations for log analysis.",
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
                                "provider": "ollama",
                                "model_name": [
                                    "llama3.1:8b",
                                    "mistral:7b"
                                ]
                            },
                            {
                                "provider": "openai",
                                "model_name": [
                                    "gpt-5",
                                    "gpt-5-mini",
                                    "gpt-5-nano"
                                ]
                            }
                        ]
                    }
                }
            }
        }
    }
}