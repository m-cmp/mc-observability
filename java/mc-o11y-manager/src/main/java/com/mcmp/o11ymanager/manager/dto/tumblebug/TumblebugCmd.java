package com.mcmp.o11ymanager.manager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TumblebugCmd {

    @JsonProperty("command")
    private List<String> command;

    @JsonProperty("user_name")
    private String userName;
}
