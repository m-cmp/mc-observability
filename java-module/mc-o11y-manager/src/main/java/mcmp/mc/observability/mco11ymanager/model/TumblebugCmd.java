package mcmp.mc.observability.mco11ymanager.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TumblebugCmd {
    private List<String> command;
    private String userName;
}
