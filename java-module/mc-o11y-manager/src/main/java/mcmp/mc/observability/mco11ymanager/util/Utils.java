package mcmp.mc.observability.mco11ymanager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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

        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            logger.debug("Command Output: {}", line);
            sb.append(line);
            sb.append("\n");
        }

        process.waitFor();

        return sb.toString();
    }
}
