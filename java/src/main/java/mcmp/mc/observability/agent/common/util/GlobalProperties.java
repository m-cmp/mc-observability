package mcmp.mc.observability.agent.common.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

@Getter
@Slf4j
@Configuration
public class GlobalProperties {
    public String uuid;

    @PostConstruct
    private void createUuidFile() {

        try {
            File file = new File(Constants.AGENT_UUID_PATH);
            if(!file.exists() || file.length() == 0) {
                if(!file.exists()) {
                    file.createNewFile();
                }
                uuid = UUID.randomUUID().toString();
                FileWriter fw = new FileWriter(file);
                BufferedWriter writer = new BufferedWriter(fw);

                writer.write(uuid);
                writer.close();
            } else {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                uuid = reader.readLine();
                reader.close();
            }

            System.setProperty(Constants.UUID_PROPERTY_KEY, uuid);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
