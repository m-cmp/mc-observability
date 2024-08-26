package mcmp.mc.observability;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(basePackages = "mcmp.mc.observability")
@MapperScan
@EnableScheduling
@SpringBootApplication
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
