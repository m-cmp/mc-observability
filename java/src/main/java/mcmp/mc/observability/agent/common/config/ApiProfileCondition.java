package mcmp.mc.observability.agent.common.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ApiProfileCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String prop = context.getEnvironment().getProperty("spring.config.on-profile");
        return !(prop != null && prop.contains("api"));
    }
}