package com.mcmp.o11ymanager.manager.entity;

import java.io.Serializable;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class VmId implements Serializable {
    private String nsId;
    private String mciId;
    private String vmId;
}
