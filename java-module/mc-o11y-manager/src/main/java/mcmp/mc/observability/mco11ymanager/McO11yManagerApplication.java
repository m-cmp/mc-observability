package mcmp.mc.observability.mco11ymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

@EnableFeignClients
@SpringBootApplication
public class McO11yManagerApplication {
    public static void main(String[] args) {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        String pidFilePath = "mc-o11y-manager.pid";

        try (FileWriter fileWriter = new FileWriter(pidFilePath)) {
            fileWriter.write(pid);
            System.out.println("PID file created: " + pidFilePath);
        } catch (IOException e) {
            System.err.println("Error creating PID file: " + e.getMessage());
        }

        SpringApplication.run(McO11yManagerApplication.class, args);
    }
}
