package mcmp.mc.observability.agent.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@EnableSwagger2
@Configuration
public class SwaggerConfig {
    @Value("${project.version:}")
    private String version;

    @Bean
    public Docket api() {
        Set<String> defaultProducesAndConsumes = new HashSet<>(Collections.singletonList("application/json"));

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder().version(version).description("Api Documentation").title("Api Documentation").build())
                .select()
                .apis(RequestHandlerSelectors.basePackage(
                        "mcmp.mc.observability"
                ))
                .paths(PathSelectors.any())
                .build()
                .useDefaultResponseMessages(false)
                .produces(defaultProducesAndConsumes);
    }
}