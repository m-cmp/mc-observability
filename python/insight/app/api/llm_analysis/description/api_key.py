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
                        "data": [{"seq": 1, "provider": "openai", "api_key": "api_key"}],
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
                        "data": {"seq": 2, "provider": "openai", "api_key": "api_key"},
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
