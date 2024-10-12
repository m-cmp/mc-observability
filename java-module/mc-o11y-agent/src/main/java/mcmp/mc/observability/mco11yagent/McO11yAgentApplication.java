package mcmp.mc.observability.mco11yagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

@MapperScan
@EnableScheduling
@EnableFeignClients(basePackages = "mcmp.mc.observability.mco11yagent")
@SpringBootApplication
public class McO11yAgentApplication {
    public static void main(String[] args) {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        String pidFilePath = "mc-o11y-agent.pid";

        try (FileWriter fileWriter = new FileWriter(pidFilePath)) {
            fileWriter.write(pid);
            System.out.println("PID file created: " + pidFilePath);
        } catch (IOException e) {
            System.err.println("Error creating PID file: " + e.getMessage());
        }

        SpringApplication.run(McO11yAgentApplication.class, args);
    }
}
