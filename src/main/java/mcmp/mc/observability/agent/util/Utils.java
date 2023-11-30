package mcmp.mc.observability.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

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

    public static String readFile(String path) throws IOException {
        String content = Files.lines(Paths.get(path), StandardCharsets.UTF_8)
                .collect(Collectors.joining(System.lineSeparator()));

        return content;
    }

    public static void writeFile(String context, String path) throws IOException {
        File file = new File(path);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(context);
        writer.close();
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if(file.exists())
            file.delete();
    }
}
