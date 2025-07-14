package com.mcmp.o11ymanager;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients
@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
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
