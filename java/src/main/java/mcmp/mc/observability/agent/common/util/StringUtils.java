package mcmp.mc.observability.agent.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    public static final String CONFIG_REGEX = "\\s*=\\s*(?:\\[\"|\\\")(.+?)(?:\"\\]|\\\")";

    public static String extractConfigValue(String string, String key) {
        return extractValue(string, key + CONFIG_REGEX);
    }

    public static String extractValue(String string, String pattern) {
        Pattern r = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = r.matcher(string);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }
}
