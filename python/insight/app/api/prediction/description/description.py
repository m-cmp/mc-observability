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
                                "VM",
                                "MCI"
                            ],
                            "metric_types": [
                                "CPU",
                                "MEM",
                                "Disk",
                                "System Load"
                            ],
                            "prediction_ranges": {
                                "min": "1h",
                                "max": "2160h"
                            }
                        },
                        "rsCode": "200",
                        "rsMsg": "Success"
                    }
                }
            }
        }
    }
}


post_prediction_description = {
    "api_description": "Predict future metrics (CPU, MEM, Disk, System Load) for a given VM or MCI group.",
    "response": {
        "200": {
            "description": "Successful prediction response",
            "content": {
                "application/json": {
                    "example": {
                        "data": {
                            "nsId": "string",
                            "targetId": "string",
                            "metric_type": "CPU",
                            "target_type": "VM",
                            "values": [
                                {
                                    "timestamp": "2024-08-22T00:00:00Z",
                                    "predicted_value": 75
                                },
                                {
                                    "timestamp": "2024-08-22T01:00:00Z",
                                    "predicted_value": 78
                                },
                                {
                                    "timestamp": "2024-08-22T02:00:00Z",
                                    "predicted_value": 80
                                }
                            ]
                        },
                        "rsCode": "200",
                        "rsMsg": "Success"
                    }
                }
            }
        }
    }
}


get_history_description = {
    "api_description": "Get previously stored prediction data for a specific VM or MCI group.",
    "response": {
        "200": {
            "description": "Successfully retrieved prediction history",
            "content": {
                "application/json": {
                    "example": {
                        "data": {
                            "nsId": "string",
                            "targetId": "string",
                            "metric_type": "CPU",
                            "values": [
                                {
                                    "timestamp": "2024-08-22T00:00:00Z",
                                    "predicted_value": 50
                                },
                                {
                                    "timestamp": "2024-08-22T01:00:00Z",
                                    "predicted_value": 55
                                },
                                {
                                    "timestamp": "2024-08-22T02:00:00Z",
                                    "predicted_value": 60
                                }
                            ]
                        },
                        "rsCode": "200",
                        "rsMsg": "Success"
                    }
                }
            }
        }
    }
}