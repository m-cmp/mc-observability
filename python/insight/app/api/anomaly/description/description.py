get_options_description = {
    "api_description": "Fetch the available target types, metric types, and interval options for the anomaly detection API.",
    "response": {
        "200": {
            "description": "Successful Response",
            "content": {
                "application/json": {
                    "example": {
                        "data": {
                            "target_types": ["vm", "mci"],
                            "measurements": ["cpu", "mem"],
                            "execution_intervals": ["5m", "10m", "30m"],
                        },
                        "rs_code": "200",
                        "rs_msg": "Success",
                    }
                }
            },
        }
    },
}

get_settings_description = {
    "api_description": "Fetch the current settings for all anomaly detection targets.",
    "response": {},
}

post_settings_description = {
    "api_description": "Register a target for anomaly detection and automatically schedule detection tasks.",
    "response": {},
}

put_settings_description = {
    "api_description": "Modify the settings for a specific anomaly detection target, including the monitoring metric and interval.",
    "response": {},
}

delete_settings_description = {
    "api_description": "Remove a target from anomaly detection, stopping and removing any scheduled tasks.",
    "response": {},
}

get_specific_settings_mci_description = {
    "api_description": "Fetch the current anomaly detection settings for a specific mci group.",
    "response": {},
}

get_specific_settings_vm_description = {
    "api_description": "Fetch the current anomaly detection settings for a specific vm.",
    "response": {},
}

get_history_mci_description = {
    "api_description": "Fetch the results of anomaly detection for mci group within a given time range.",
    "response": {},
}

get_history_vm_description = {
    "api_description": "Fetch the results of anomaly detection for a specific vm within a given time range.",
    "response": {},
}

post_anomaly_detection_description = {"api_description": "Request anomaly detection", "response": {}}

get_anomaly_detection_measurements_description = {
    "api_description": "Get measurements, field lists available for the feature",
    "response": {
        "200": {
            "description": "Successfully retrieved anomaly detection measurements",
            "content": {
                "application/json": {
                    "example": {
                        "data": [
                            {
                                "plugin_seq": 1,
                                "measurement": "cpu",
                                "fields": [{"field_key": "usage_idle", "unit": "percent"}],
                            },
                            {
                                "plugin_seq": 4,
                                "measurement": "mem",
                                "fields": [{"field_key": "used_percent", "unit": "percent"}],
                            },
                        ],
                        "rs_code": "200",
                        "rs_msg": "Success",
                    }
                }
            },
        }
    },
}

get_specific_measurement_description = {
    "api_description": "Get Field list of specific measurements available for that feature",
    "response": {
        "200": {
            "description": "Successfully retrieved anomaly detection field list of specific measurement",
            "content": {
                "application/json": {
                    "example": {
                        "data": {
                            "plugin_seq": 1,
                            "measurement": "cpu",
                            "fields": [{"field_key": "usage_idle", "unit": "percent"}],
                        },
                        "rs_code": "200",
                        "rs_msg": "Success",
                    }
                }
            },
        }
    },
}
