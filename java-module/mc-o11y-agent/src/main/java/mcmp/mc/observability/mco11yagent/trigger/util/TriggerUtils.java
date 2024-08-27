package mcmp.mc.observability.mco11yagent.trigger.util;

public class TriggerUtils {
    public static String camelToSnake(String camel) {
        StringBuilder snake = new StringBuilder();
        snake.append(Character.toLowerCase(camel.charAt(0)));

        for( int i = 1 ; i < camel.length() ; i++ ) {
            char c = camel.charAt(i);
            if(Character.isUpperCase(c)) {
                snake.append("_").append(Character.toLowerCase(c));
            }
            else {
                snake.append(c);
            }
        }
        return snake.toString();
    }
}
