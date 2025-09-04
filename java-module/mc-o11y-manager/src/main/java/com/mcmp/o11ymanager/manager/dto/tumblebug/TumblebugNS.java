package com.mcmp.o11ymanager.manager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TumblebugNS {

    @JsonProperty("ns")
    private List<NS> ns;

    @Getter
    @Setter
    public static class NS {

        @JsonProperty("id")
        private String id;

        @JsonProperty("description")
        private String description;

        @JsonProperty("name")
        private String name;

        private String resourceType;

        @JsonProperty("uid")
        private String uid;
    }
}
