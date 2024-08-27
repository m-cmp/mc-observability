package mcmp.mc.observability.mco11yagent.monitoring.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String readFile(String path) throws IOException {
        return Files.lines(Paths.get(path), StandardCharsets.UTF_8)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public static void writeFile(String context, String path) throws IOException {
        File file = new File(path);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(context);
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getMessage(e));
        }
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if(file.exists()) file.delete();
    }

    public static String runExec(String[] command) throws IOException, InterruptedException {
        StringBuilder commandLog = new StringBuilder();
        for (String token : command) {
            commandLog.append(" ").append(token);
        }
        logger.debug("Process Executing : {}", commandLog);

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        InputStream isErr = process.getErrorStream();
        InputStreamReader isrErr = new InputStreamReader(isErr, StandardCharsets.UTF_8);
        BufferedReader brErr = new BufferedReader(isrErr);

        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            logger.debug("Command Output: {}", line);
            sb.append(line);
            sb.append("\n");
        }

        while ((line = brErr.readLine()) != null) {
            logger.error("Error Output: {}", line);
            sb.append(line);
            sb.append("\n");
        }

        process.waitFor();

        return sb.toString();
    }
}
