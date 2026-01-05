package com.mcmp.o11ymanager.manager.infrastructure.port.winrm;

import static com.mcmp.o11ymanager.manager.constants.WinRmConstants.WINRM_HTTPS_PORT;

import com.mcmp.o11ymanager.manager.model.agentHealth.AgentCommandResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WinRmClient {

    @Value("${winrm.connection-timeout:30000}")
    private int winrmConnectionTimeout;

    private HttpClient httpClient;

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            try {
                httpClient =
                        HttpClient.newBuilder()
                                .sslContext(SSLContext.getDefault())
                                .connectTimeout(Duration.ofMillis(winrmConnectionTimeout))
                                .build();
            } catch (Exception e) {
                log.error("Failed to create HTTP client: {}", e.getMessage());
                httpClient =
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofMillis(winrmConnectionTimeout))
                                .build();
            }
        }
        return httpClient;
    }

    public AgentCommandResult executeCommand(
            String user, String ip, int port, String password, String command) {
        try {
            String protocol = port == WINRM_HTTPS_PORT ? "https" : "http";
            String url = protocol + "://" + ip + ":" + port + "/wsman";

            String shellId = createShell(url, user, password);
            if (shellId == null) {
                return AgentCommandResult.builder()
                        .output("")
                        .error("Failed to create shell")
                        .exitCode(-1)
                        .build();
            }

            try {
                String commandId = runCommand(url, user, password, shellId, command);
                if (commandId == null) {
                    return AgentCommandResult.builder()
                            .output("")
                            .error("Failed to run command")
                            .exitCode(-1)
                            .build();
                }

                return getCommandOutput(url, user, password, shellId, commandId);
            } finally {
                deleteShell(url, user, password, shellId);
            }
        } catch (Exception e) {
            log.debug("WinRM command execution failed for {}: {}", ip, e.getMessage());
            return AgentCommandResult.builder()
                    .output("")
                    .error(e.getMessage())
                    .exitCode(-1)
                    .build();
        }
    }

    private String createShell(String url, String user, String password) throws Exception {
        String messageId = UUID.randomUUID().toString();
        String body =
                """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                        xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                        xmlns:w="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd"
                        xmlns:p="http://schemas.microsoft.com/wbem/wsman/1/wsman.xsd">
                <s:Header>
                    <a:To>%s</a:To>
                    <a:ReplyTo><a:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address></a:ReplyTo>
                    <w:MaxEnvelopeSize s:mustUnderstand="true">153600</w:MaxEnvelopeSize>
                    <a:MessageID>uuid:%s</a:MessageID>
                    <w:Locale xml:lang="en-US" s:mustUnderstand="false"/>
                    <p:DataLocale xml:lang="en-US" s:mustUnderstand="false"/>
                    <w:OperationTimeout>PT60S</w:OperationTimeout>
                    <w:ResourceURI s:mustUnderstand="true">http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd</w:ResourceURI>
                    <a:Action s:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</a:Action>
                    <w:OptionSet>
                        <w:Option Name="WINRS_NOPROFILE">TRUE</w:Option>
                        <w:Option Name="WINRS_CODEPAGE">65001</w:Option>
                    </w:OptionSet>
                </s:Header>
                <s:Body>
                    <rsp:Shell xmlns:rsp="http://schemas.microsoft.com/wbem/wsman/1/windows/shell">
                        <rsp:InputStreams>stdin</rsp:InputStreams>
                        <rsp:OutputStreams>stdout stderr</rsp:OutputStreams>
                    </rsp:Shell>
                </s:Body>
            </s:Envelope>
            """
                        .formatted(url, messageId);

        String response = sendRequest(url, user, password, body);
        Pattern pattern = Pattern.compile("<rsp:ShellId>([^<]+)</rsp:ShellId>");
        Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String runCommand(
            String url, String user, String password, String shellId, String command)
            throws Exception {
        String messageId = UUID.randomUUID().toString();
        String body =
                """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                        xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                        xmlns:w="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd"
                        xmlns:p="http://schemas.microsoft.com/wbem/wsman/1/wsman.xsd">
                <s:Header>
                    <a:To>%s</a:To>
                    <a:ReplyTo><a:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address></a:ReplyTo>
                    <w:MaxEnvelopeSize s:mustUnderstand="true">153600</w:MaxEnvelopeSize>
                    <a:MessageID>uuid:%s</a:MessageID>
                    <w:Locale xml:lang="en-US" s:mustUnderstand="false"/>
                    <p:DataLocale xml:lang="en-US" s:mustUnderstand="false"/>
                    <w:OperationTimeout>PT60S</w:OperationTimeout>
                    <w:ResourceURI s:mustUnderstand="true">http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd</w:ResourceURI>
                    <a:Action s:mustUnderstand="true">http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Command</a:Action>
                    <w:SelectorSet>
                        <w:Selector Name="ShellId">%s</w:Selector>
                    </w:SelectorSet>
                </s:Header>
                <s:Body>
                    <rsp:CommandLine xmlns:rsp="http://schemas.microsoft.com/wbem/wsman/1/windows/shell">
                        <rsp:Command>powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -EncodedCommand %s</rsp:Command>
                    </rsp:CommandLine>
                </s:Body>
            </s:Envelope>
            """
                        .formatted(
                                url,
                                messageId,
                                shellId,
                                Base64.getEncoder()
                                        .encodeToString(
                                                command.getBytes(StandardCharsets.UTF_16LE)));

        String response = sendRequest(url, user, password, body);
        Pattern pattern = Pattern.compile("<rsp:CommandId>([^<]+)</rsp:CommandId>");
        Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    private AgentCommandResult getCommandOutput(
            String url, String user, String password, String shellId, String commandId)
            throws Exception {
        String messageId = UUID.randomUUID().toString();
        String body =
                """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                        xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                        xmlns:w="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd"
                        xmlns:p="http://schemas.microsoft.com/wbem/wsman/1/wsman.xsd">
                <s:Header>
                    <a:To>%s</a:To>
                    <a:ReplyTo><a:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address></a:ReplyTo>
                    <w:MaxEnvelopeSize s:mustUnderstand="true">153600</w:MaxEnvelopeSize>
                    <a:MessageID>uuid:%s</a:MessageID>
                    <w:Locale xml:lang="en-US" s:mustUnderstand="false"/>
                    <p:DataLocale xml:lang="en-US" s:mustUnderstand="false"/>
                    <w:OperationTimeout>PT60S</w:OperationTimeout>
                    <w:ResourceURI s:mustUnderstand="true">http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd</w:ResourceURI>
                    <a:Action s:mustUnderstand="true">http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Receive</a:Action>
                    <w:SelectorSet>
                        <w:Selector Name="ShellId">%s</w:Selector>
                    </w:SelectorSet>
                </s:Header>
                <s:Body>
                    <rsp:Receive xmlns:rsp="http://schemas.microsoft.com/wbem/wsman/1/windows/shell">
                        <rsp:DesiredStream CommandId="%s">stdout stderr</rsp:DesiredStream>
                    </rsp:Receive>
                </s:Body>
            </s:Envelope>
            """
                        .formatted(url, messageId, shellId, commandId);

        String response = sendRequest(url, user, password, body);

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Pattern stdoutPattern =
                Pattern.compile("<rsp:Stream Name=\"stdout\"[^>]*>([^<]*)</rsp:Stream>");
        Matcher stdoutMatcher = stdoutPattern.matcher(response);
        while (stdoutMatcher.find()) {
            String encoded = stdoutMatcher.group(1);
            if (!encoded.isEmpty()) {
                stdout.append(
                        new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
            }
        }

        Pattern stderrPattern =
                Pattern.compile("<rsp:Stream Name=\"stderr\"[^>]*>([^<]*)</rsp:Stream>");
        Matcher stderrMatcher = stderrPattern.matcher(response);
        while (stderrMatcher.find()) {
            String encoded = stderrMatcher.group(1);
            if (!encoded.isEmpty()) {
                stderr.append(
                        new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
            }
        }

        int exitCode = 0;
        Pattern exitCodePattern = Pattern.compile("<rsp:ExitCode>([^<]+)</rsp:ExitCode>");
        Matcher exitCodeMatcher = exitCodePattern.matcher(response);
        if (exitCodeMatcher.find()) {
            exitCode = Integer.parseInt(exitCodeMatcher.group(1));
        }

        return AgentCommandResult.builder()
                .output(stdout.toString().trim())
                .error(stderr.toString().trim())
                .exitCode(exitCode)
                .build();
    }

    private void deleteShell(String url, String user, String password, String shellId) {
        try {
            String messageId = UUID.randomUUID().toString();
            String body =
                    """
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                            xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                            xmlns:w="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd">
                    <s:Header>
                        <a:To>%s</a:To>
                        <a:ReplyTo><a:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address></a:ReplyTo>
                        <w:MaxEnvelopeSize s:mustUnderstand="true">153600</w:MaxEnvelopeSize>
                        <a:MessageID>uuid:%s</a:MessageID>
                        <w:Locale xml:lang="en-US" s:mustUnderstand="false"/>
                        <w:OperationTimeout>PT60S</w:OperationTimeout>
                        <w:ResourceURI s:mustUnderstand="true">http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd</w:ResourceURI>
                        <a:Action s:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</a:Action>
                        <w:SelectorSet>
                            <w:Selector Name="ShellId">%s</w:Selector>
                        </w:SelectorSet>
                    </s:Header>
                    <s:Body/>
                </s:Envelope>
                """
                            .formatted(url, messageId, shellId);
            sendRequest(url, user, password, body);
        } catch (Exception e) {
            log.debug("Failed to delete shell: {}", e.getMessage());
        }
    }

    private String sendRequest(String url, String user, String password, String body)
            throws Exception {
        String auth =
                Base64.getEncoder()
                        .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/soap+xml;charset=UTF-8")
                        .header("Authorization", "Basic " + auth)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofMillis(winrmConnectionTimeout))
                        .build();

        HttpResponse<String> response =
                getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "WinRM request failed with status "
                            + response.statusCode()
                            + ": "
                            + response.body());
        }

        return response.body();
    }
}
