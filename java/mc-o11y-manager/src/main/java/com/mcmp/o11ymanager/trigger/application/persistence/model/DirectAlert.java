package com.mcmp.o11ymanager.trigger.application.persistence.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DirectAlert {

    private String title;
    private String message;
    private String channelName;
    private List<String> recipients;
}
