package mcmp.mc.observability.agent.trigger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    public String host;

    @Value("${spring.mail.port}")
    public int port;

    @Value("${spring.mail.username}")
    public String username;

    @Value("${spring.mail.password}")
    public String password;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    public boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    public boolean starttlsEnable;


    public JavaMailSender getJavaMailSender(){
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.debug", "true");

        return mailSender;
    }

}

