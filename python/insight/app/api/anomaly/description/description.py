get_options_description = {
    "api_description": "Fetch the available target types, metric types, and interval options for the anomaly detection API.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "data": {
                            "target_types": [
                                "vm",
                                "mci"
                            ],
                            "measurements": [
                                "cpu",
                                "mem"
                            ],
                            "execution_intervals": [
                                "5m",
                                "10m",
                                "30m"
                            ]
                        },
                        "rs_code": "200",
                        "rs_msg": "Success"
                    }
                }
            }
        }
    }
}

get_settings_description = {
    "api_description": "Fetch the current settings for all anomaly detection targets.",
    "response": {}
}

post_settings_description = {
    "api_description": "Register a target for anomaly detection and automatically schedule detection tasks.",
    "response": {}
}

put_settings_description = {
    "api_description": "Modify the settings for a specific anomaly detection target, including the monitoring metric and interval.",
    "response": {}
}

delete_settings_description = {
    "api_description": "Remove a target from anomaly detection, stopping and removing any scheduled tasks.",
    "response": {}
}

get_specific_settings_description = {
    "api_description": "Fetch the current settings for a specific anomaly detection target.",
    "response": {}
}

get_history_description = {
    "api_description": "Fetch the results of anomaly detection for a specific target within a given time range.",
    "response": {}
}

post_anomaly_detection_description = {
    "api_description": "Request anomaly detection",
    "response": {}
}
