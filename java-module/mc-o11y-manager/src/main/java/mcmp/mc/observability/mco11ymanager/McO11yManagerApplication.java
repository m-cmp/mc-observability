package mcmp.mc.observability.mco11ymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class McO11yManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McO11yManagerApplication.class, args);
    }

}
