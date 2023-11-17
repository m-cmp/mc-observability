package mcmp.mc.observability.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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
