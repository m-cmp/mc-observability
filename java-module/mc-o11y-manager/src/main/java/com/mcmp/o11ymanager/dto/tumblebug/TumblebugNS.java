package com.mcmp.o11ymanager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

        @JsonProperty("resource_type")
        private String resourceType;

        @JsonProperty("uid")
        private String uid;
    }
}
