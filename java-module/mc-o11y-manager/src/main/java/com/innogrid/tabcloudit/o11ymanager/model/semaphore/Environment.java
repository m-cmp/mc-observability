package com.innogrid.tabcloudit.o11ymanager.model.semaphore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Environment {
    private final Map<String, String> variables;

    public Environment() {
        this.variables = new HashMap<>();
    }

    public Environment addVariable(String key, String value) {
        variables.put(key, value);
        return this;
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(variables);
    }
}
