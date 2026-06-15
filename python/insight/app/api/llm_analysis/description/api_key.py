# OpenAI API Key descriptions
get_api_key_description = {
    "api_description": "Retrieve the current API key configuration.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": [
                            {"seq": 1, "provider": "openai", "api_key": "api_key"},
                            {
                                "seq": 2,
                                "provider": "openai-compatible",
                                "api_key": "api_key",
                                "base_url": "http://vllm:8000/v1",
                            },
                        ],
                    }
                }
            },
        }
    },
}

post_api_key_description = {
    "api_description": "Save or update the API key configuration.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": {
                            "seq": 2,
                            "provider": "openai-compatible",
                            "api_key": "api_key",
                            "base_url": "http://vllm:8000/v1",
                        },
                    }
                }
            },
        }
    },
}

delete_api_key_description = {
    "api_description": "Delete the API key configuration.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "rs_code": "200",
                        "rs_msg": "Success",
                        "data": {"seq": 3, "provider": "openai", "api_key": "api_key"},
                    }
                }
            },
        }
    },
}
