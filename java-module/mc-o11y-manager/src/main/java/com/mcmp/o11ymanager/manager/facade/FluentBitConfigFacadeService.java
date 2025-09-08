package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FluentBitConfigFacadeService {

    private final FileService fileService;

    @Value("${loki.url}")
    private String lokiURL;

    private final ClassPathResource fluentBitVariables =
            new ClassPathResource("fluent-bit_variables");

    public String initFluentbitConfig(String nsId, String mciId, String vmId) {
        String template = fileService.getFileContent(fluentBitVariables);

        String[] lokiURLSplit = lokiURL.split("://");
        if (lokiURLSplit.length != 2) {
            throw new RuntimeException("Manager's Loki URL is invalid!");
        }
        lokiURLSplit = lokiURLSplit[1].split(":");
        if (lokiURLSplit.length < 1) {
            throw new RuntimeException("Manager's Loki URL is invalid!");
        }

        String lokiHost = lokiURLSplit[0];

        StringBuilder sb = new StringBuilder();

        fileService.appendConfig(fluentBitVariables, sb);

        nsId = nsId != null ? nsId : "";
        mciId = mciId != null ? mciId : "";
        vmId = vmId != null ? vmId : "";
        lokiHost = lokiHost != null ? lokiHost : "";

        log.debug("VM={}/{}/{}", nsId, mciId, vmId);

        return template.replace("@NS_ID", nsId)
                .replace("@MCI_ID", mciId)
                .replace("@VM_ID", vmId)
                .replace("@LOKI_HOST", lokiHost);
    }
}
