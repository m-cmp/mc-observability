get_options_description = {
    "api_description": "Fetch the available target types, metric types, and prediction range options for the prediction API.",
    "response": {
        "200": {
            "description": "Successful retrieval of prediction options",
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
                                "mem",
                                "disk",
                                "system"
                            ],
                            "prediction_ranges": {
                                "min": "1h",
                                "max": "2160h"
                            }
                        },
                        "rs_code": "200",
                        "rs_msg": "Success"
                    }
                }
            }
        }
    }
}


post_prediction_description = {
    "api_description": "Predict future metrics (cpu, mem, disk, system) for a given vm or mci group.",
    "response": {
        "200": {
            "description": "Successful prediction response",
            "content": {
                "application/json": {
                    "example": {
                        "data": {
                            "ns_id": "string",
                            "target_id": "string",
                            "measurement": "cpu",
                            "target_type": "vm",
                            "values": [
                                {
                                    "timestamp": "2024-08-22T00:00:00Z",
                                    "value": 75
                                },
                                {
                                    "timestamp": "2024-08-22T01:00:00Z",
                                    "value": 78
                                },
                                {
                                    "timestamp": "2024-08-22T02:00:00Z",
                                    "value": 80
                                }
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


get_history_description = {
    "api_description": "Get previously stored prediction data for a specific vm or mci group.",
    "response": {
        "200": {
            "description": "Successfully retrieved prediction history",
            "content": {
                "application/json": {
                    "example": {
                        "data": {
                            "ns_id": "string",
                            "target_id": "string",
                            "measurement": "cpu",
                            "values": [
                                {
                                    "timestamp": "2024-08-22T00:00:00Z",
                                    "value": 50
                                },
                                {
                                    "timestamp": "2024-08-22T01:00:00Z",
                                    "value": 55
                                },
                                {
                                    "timestamp": "2024-08-22T02:00:00Z",
                                    "value": 60
                                }
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