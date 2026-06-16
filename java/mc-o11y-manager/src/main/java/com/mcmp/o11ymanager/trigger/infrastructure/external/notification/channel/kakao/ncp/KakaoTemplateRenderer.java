package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationConfigurationException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a registered Kakao AlimTalk template by substituting {@code #{key}} placeholders with the
 * provided values. Fails fast if the template references a variable that the application cannot
 * supply, surfacing template/variable drift before the message is sent.
 */
public final class KakaoTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("#\\{\\s*([a-zA-Z0-9_]+)\\s*}");

    private KakaoTemplateRenderer() {}

    /**
     * Substitutes {@code #{key}} placeholders in the template with values from the given map.
     *
     * @param templateCode the template code, used only for error messages
     * @param template the registered template content
     * @param values placeholder key to value mapping
     * @return the rendered content
     * @throws NotificationConfigurationException if the template uses an unsupported variable
     */
    public static String render(String templateCode, String template, Map<String, String> values) {
        Set<String> unknown = unknownVariables(template, values.keySet());
        if (!unknown.isEmpty()) {
            throw new NotificationConfigurationException(
                    "Kakao template '"
                            + templateCode
                            + "' uses unsupported variables "
                            + unknown
                            + "; supported: "
                            + values.keySet());
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static Set<String> unknownVariables(String template, Set<String> allowedKeys) {
        Set<String> unknown = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!allowedKeys.contains(name)) {
                unknown.add(name);
            }
        }
        return unknown;
    }
}
