package mcmp.mc.observability.mco11yagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan
@EnableScheduling
@SpringBootApplication
public class McO11yAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(McO11yAgentApplication.class, args);
    }

}
