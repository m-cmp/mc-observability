package mcmp.mc.observability.agent.common.util;

import mcmp.mc.observability.agent.monitoring.enums.CmdResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
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
        return Files.lines(Paths.get(path), StandardCharsets.UTF_8)
                .collect(Collectors.joining(System.lineSeparator()));
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


    public static CmdResult runCommand(String[] command, Output output)
            throws IOException, InterruptedException {
        String commandLog = "";
        for (String token : command) {
            commandLog += " " + token;
        }
        logger.debug("Process Executing : " + commandLog);

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        InputStream isErr = process.getErrorStream();
        InputStreamReader isrErr = new InputStreamReader(isErr, "UTF-8");
        BufferedReader brErr = new BufferedReader(isrErr);

        String line;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = br.readLine()) != null) {
            logger.debug("Command Output: " + line);
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }

        while ((line = brErr.readLine()) != null) {
            logger.error("Error Output: " + line);
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }

        output.setText(stringBuilder.toString());
        process.waitFor();

        CmdResult result = CmdResult.FAILED;
        if (process.exitValue() == 0) {
            result = CmdResult.SUCCESS;
        }
        return result;
    }
}
