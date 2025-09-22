// ✅ PredictionMeasurement
.fieldWithPath("plugin_seq").type(JsonFieldType.NUMBER).description("Plugin sequence").attributes(key("example").value(1))
.fieldWithPath("measurement").type(JsonFieldType.STRING).description("Measurement name").attributes(key("example").value("cpu"))
.fieldWithPath("fields").type(JsonFieldType.ARRAY).description("List of field definitions")
.fieldWithPath("fields[].field_key").type(JsonFieldType.STRING).description("Field key").attributes(key("example").value("usage_idle"))
.fieldWithPath("fields[].unit").type(JsonFieldType.STRING).description("Unit of measurement").attributes(key("example").value("percent"))

// ✅ PredictionOptions
.fieldWithPath("target_types").type(JsonFieldType.ARRAY).description("Available target types").attributes(key("example").value("[\"vm\", \"mci\"]"))
.fieldWithPath("measurements").type(JsonFieldType.ARRAY).description("Available measurements").attributes(key("example").value("[\"cpu\", \"mem\", \"disk\"]"))
.fieldWithPath("prediction_ranges").type(JsonFieldType.OBJECT).description("Prediction range configuration")
.fieldWithPath("prediction_ranges.min").type(JsonFieldType.STRING).description("Minimum range").attributes(key("example").value("1h"))
.fieldWithPath("prediction_ranges.max").type(JsonFieldType.STRING).description("Maximum range").attributes(key("example").value("2160h"))

// ✅ PredictionResult
.fieldWithPath("ns_id").type(JsonFieldType.STRING).description("Namespace ID").attributes(key("example").value("ns-001"))
.fieldWithPath("target_id").type(JsonFieldType.STRING).description("Target ID").attributes(key("example").value("vm-123"))
.fieldWithPath("measurement").type(JsonFieldType.STRING).description("Measurement type").attributes(key("example").value("cpu"))
.fieldWithPath("target_type").type(JsonFieldType.STRING).description("Target type").attributes(key("example").value("vm"))
.fieldWithPath("values").type(JsonFieldType.ARRAY).description("Predicted values")
.fieldWithPath("values[].timestamp").type(JsonFieldType.STRING).description("Prediction timestamp").attributes(key("example").value("2024-08-22T00:00:00Z"))
.fieldWithPath("values[].value").type(JsonFieldType.NUMBER).description("Predicted value").attributes(key("example").value(75))

// ✅ PredictionHistory
.fieldWithPath("ns_id").type(JsonFieldType.STRING).description("Namespace ID").attributes(key("example").value("ns-001"))
.fieldWithPath("target_id").type(JsonFieldType.STRING).description("Target ID").attributes(key("example").value("vm-123"))
.fieldWithPath("measurement").type(JsonFieldType.STRING).description("Measurement type").attributes(key("example").value("cpu"))
.fieldWithPath("values").type(JsonFieldType.ARRAY).description("Historical prediction values")
.fieldWithPath("values[].timestamp").type(JsonFieldType.STRING).description("History timestamp").attributes(key("example").value("2024-08-22T00:00:00Z"))
.fieldWithPath("values[].value").type(JsonFieldType.NUMBER).description("History value").attributes(key("example").value(55))

// ✅ AnomalyDetectionMeasurement
.fieldWithPath("plugin_seq").type(JsonFieldType.NUMBER).description("Plugin sequence").attributes(key("example").value(1))
.fieldWithPath("measurement").type(JsonFieldType.STRING).description("Measurement name").attributes(key("example").value("cpu"))
.fieldWithPath("fields").type(JsonFieldType.ARRAY).description("List of field definitions")
.fieldWithPath("fields[].field_key").type(JsonFieldType.STRING).description("Field key").attributes(key("example").value("usage_idle"))
.fieldWithPath("fields[].unit").type(JsonFieldType.STRING).description("Unit of measurement").attributes(key("example").value("percent"))

// ✅ AnomalyDetectionOptions
.fieldWithPath("target_types").type(JsonFieldType.ARRAY).description("Available target types").attributes(key("example").value("[\"vm\", \"mci\"]"))
.fieldWithPath("measurements").type(JsonFieldType.ARRAY).description("Available measurements").attributes(key("example").value("[\"cpu\", \"mem\"]"))
.fieldWithPath("execution_intervals").type(JsonFieldType.ARRAY).description("Execution intervals").attributes(key("example").value("[\"5m\", \"10m\", \"30m\"]"))

// ✅ AnomalyDetectionSettings
.fieldWithPath("seq").type(JsonFieldType.NUMBER).description("Setting sequence").attributes(key("example").value(1))
.fieldWithPath("ns_id").type(JsonFieldType.STRING).description("Namespace ID").attributes(key("example").value("ns-001"))
.fieldWithPath("target_id").type(JsonFieldType.STRING).description("Target ID").attributes(key("example").value("vm-123"))
.fieldWithPath("target_type").type(JsonFieldType.STRING).description("Target type").attributes(key("example").value("vm"))
.fieldWithPath("measurement").type(JsonFieldType.STRING).description("Measurement type").attributes(key("example").value("cpu"))
.fieldWithPath("execution_interval").type(JsonFieldType.STRING).description("Execution interval").attributes(key("example").value("5m"))
.fieldWithPath("last_execution").type(JsonFieldType.STRING).description("Last execution time").attributes(key("example").value("2024-10-08T06:50:37Z"))
.fieldWithPath("create_at").type(JsonFieldType.STRING).description("Created at").attributes(key("example").value("2024-10-08T06:50:37Z"))

// ✅ AnomalyDetectionHistory
.fieldWithPath("ns_id").type(JsonFieldType.STRING).description("Namespace ID").attributes(key("example").value("ns-001"))
.fieldWithPath("target_id").type(JsonFieldType.STRING).description("Target ID").attributes(key("example").value("vm-123"))
.fieldWithPath("measurement").type(JsonFieldType.STRING).description("Measurement type").attributes(key("example").value("cpu"))
.fieldWithPath("values").type(JsonFieldType.ARRAY).description("Anomaly detection results")
.fieldWithPath("values[].timestamp").type(JsonFieldType.STRING).description("Timestamp").attributes(key("example").value("2024-10-08T06:50:37Z"))
.fieldWithPath("values[].anomaly_score").type(JsonFieldType.NUMBER).description("Anomaly score").attributes(key("example").value(0.75))
.fieldWithPath("values[].is_anomaly").type(JsonFieldType.NUMBER).description("Is anomaly (0/1)").attributes(key("example").value(1))
.fieldWithPath("values[].value").type(JsonFieldType.NUMBER).description("Observed value").attributes(key("example").value(85))

// ✅ LLMChatSession
.fieldWithPath("seq").type(JsonFieldType.NUMBER).description("Session sequence").attributes(key("example").value(1))
.fieldWithPath("user_id").type(JsonFieldType.STRING).description("User ID").attributes(key("example").value("1"))
.fieldWithPath("session_id").type(JsonFieldType.STRING).description("Session ID").attributes(key("example").value("986ef65f-e425-478a-b84c-03fa80682f36"))
.fieldWithPath("provider").type(JsonFieldType.STRING).description("Provider name").attributes(key("example").value("openai"))
.fieldWithPath("model_name").type(JsonFieldType.STRING).description("Model name").attributes(key("example").value("gpt-5-mini"))
.fieldWithPath("regdate").type(JsonFieldType.STRING).description("Registered date").attributes(key("example").value("2025-09-17T14:12:36"))

// ✅ LLMModel
.fieldWithPath("provider").type(JsonFieldType.STRING).description("Provider name").attributes(key("example").value("openai"))
.fieldWithPath("model_name").type(JsonFieldType.ARRAY).description("Available model names").attributes(key("example").value("[\"gpt-5\", \"gpt-5-mini\"]"))

// ✅ SessionHistory
.fieldWithPath("messages").type(JsonFieldType.ARRAY).description("Chat messages")
.fieldWithPath("messages[].message_type").type(JsonFieldType.STRING).description("Message type").attributes(key("example").value("human"))
.fieldWithPath("messages[].message").type(JsonFieldType.STRING).description("Message text").attributes(key("example").value("Analyze these error logs"))
.fieldWithPath("seq").type(JsonFieldType.NUMBER).description("Session sequence").attributes(key("example").value(41))
.fieldWithPath("user_id").type(JsonFieldType.STRING).description("User ID").attributes(key("example").value("1"))
.fieldWithPath("session_id").type(JsonFieldType.STRING).description("Session ID").attributes(key("example").value("fa746359-ab1b-4755-a1cc-630e373135ff"))
.fieldWithPath("provider").type(JsonFieldType.STRING).description("Provider").attributes(key("example").value("openai"))
.fieldWithPath("model_name").type(JsonFieldType.STRING).description("Model name").attributes(key("example").value("gpt-5-mini"))
.fieldWithPath("regdate").type(JsonFieldType.STRING).description("Registered date").attributes(key("example").value("2025-09-18T14:59:37"))

// ✅ Message (Log/Alert analysis)
.fieldWithPath("message_type").type(JsonFieldType.STRING).description("Message type").attributes(key("example").value("ai"))
.fieldWithPath("message").type(JsonFieldType.STRING).description("Message content").attributes(key("example").value("I've analyzed the error logs from the past hour..."))
.fieldWithPath("metadata").type(JsonFieldType.OBJECT).description("Metadata for analysis")
.fieldWithPath("metadata.queries_executed").type(JsonFieldType.ARRAY).description("Executed queries")
.fieldWithPath("metadata.total_execution_time").type(JsonFieldType.NUMBER).description("Execution time in seconds").attributes(key("example").value(2.3))
.fieldWithPath("metadata.tool_calls_count").type(JsonFieldType.NUMBER).description("Tool call count").attributes(key("example").value(3))
.fieldWithPath("metadata.databases_accessed").type(JsonFieldType.ARRAY).description("Databases accessed").attributes(key("example").value("[\"mariadb\", \"influxdb\"]"))
