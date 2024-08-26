package mcmp.mc.observability.agent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.trigger.client.KapacitorClient;
import mcmp.mc.observability.agent.trigger.model.*;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KapacitorApiService {

    private final KapacitorClient kapacitorClient;

    public KapacitorTaskInfo getTask(String kapacitorUrl, String id) {

        try {
            return kapacitorClient.getTask(getKapacitorURI(kapacitorUrl), id);
        } catch (Exception e) {
            log.error("Failed to search Kapacitor task. Task ID : {}, Error : {}", id, e.getMessage());
            return null;
        }
    }


    public List<KapacitorTaskInfo> getTaskList(String url) throws URISyntaxException {
        String kapacitorUrl = getKapacitorUrl(url);
        KapacitorTaskListInfo kapacitorTaskListInfo = kapacitorClient.getTaskList(getKapacitorURI(kapacitorUrl));
        return kapacitorTaskListInfo.getTasks();
    }

    public void createTask(TriggerPolicyInfo policyInfo, String url, String database, String retentionPolicy) {

        KapacitorTaskInfo kapacitorTaskInfo = new KapacitorTaskInfo();
        kapacitorTaskInfo.setId(String.valueOf(policyInfo.getSeq()));
        kapacitorTaskInfo.setType("stream");
        kapacitorTaskInfo.setStatus(policyInfo.getStatus().toString());

        policyInfo.setTickScriptStorageInfo(database, retentionPolicy);
        kapacitorTaskInfo.setScript(policyInfo.getTickScript());

        Map<String, String> dbrps = new HashMap<>();
        dbrps.put("db", database);
        dbrps.put("rp", retentionPolicy);
        kapacitorTaskInfo.setDbrps(Collections.singletonList(dbrps));

        String kapacitorUrl = getKapacitorUrl(url);

        try {
            KapacitorTaskInfo taskInfo = getTask(kapacitorUrl, kapacitorTaskInfo.getId());
            if(taskInfo != null)
                return;

            kapacitorClient.createTask(
                    getKapacitorURI(kapacitorUrl),
                    kapacitorTaskInfo
            );
        } catch (Exception e) {
            log.error("Failed to create Kapacitor task. TriggerPolicy Seq : {}, Storage URL : {}, Error: {}", policyInfo.getSeq(), url, e.getMessage());
        }
    }

    public boolean updateTask(TriggerPolicyInfo policyInfo, ManageTriggerTargetStorageInfo targetStorageInfo) {
        String kapacitorTaskId = String.valueOf(policyInfo.getSeq());

        Map<String, String> updateTaskParam = new HashMap<>();
        updateTaskParam.put("status", policyInfo.getStatus().toString());

        policyInfo.setTickScriptStorageInfo(targetStorageInfo.getDatabase(), targetStorageInfo.getRetentionPolicy());
        updateTaskParam.put("script", policyInfo.getTickScript());

        try {
            String kapacitorUrl = getKapacitorUrl(targetStorageInfo.getUrl());
            KapacitorTaskInfo taskInfo = getTask(kapacitorUrl, kapacitorTaskId);
            if(taskInfo == null)
                throw new ResultCodeException(ResultCode.FAILED, "Trigger Policy Not Exists");

            updateTask(kapacitorUrl, kapacitorTaskId, updateTaskParam);

            if(policyInfo.getStatus().equals("enabled")) {
                updateTask(kapacitorUrl, kapacitorTaskId, Collections.singletonMap("status", "disabled"));
                updateTask(kapacitorUrl, kapacitorTaskId, Collections.singletonMap("status", "enabled"));
            }
            return true;
        } catch (Exception e) {
            throw new ResultCodeException(ResultCode.FAILED, "Failed to update Kapacitor task. TriggerPolicy Seq : {}, Storage URL : {}, Error: {}", policyInfo.getSeq(), targetStorageInfo.getUrl(), e.getMessage());
        }
    }

    public void updateTask(String kapacitorUrl, String kapacitorTaskId, Map<String, String> params) throws URISyntaxException {
        kapacitorClient.updateTask(
                getKapacitorURI(kapacitorUrl),
                kapacitorTaskId,
                params
        );
    }

    public void deleteTask(Long policySeq, String url) {

        String kapacitorUrl = getKapacitorUrl(url);
        String kapacitorTaskId = String.valueOf(policySeq);

        try {
            KapacitorTaskInfo taskInfo = getTask(kapacitorUrl, kapacitorTaskId);
            if(taskInfo == null)
                return;

            kapacitorClient.deleteTask(
                    getKapacitorURI(kapacitorUrl),
                    kapacitorTaskId
            );
        } catch (Exception e) {
            throw new ResultCodeException(ResultCode.FAILED, "Failed to delete Kapacitor task. TriggerPolicy Seq : {}, Storage URL : {}, Error: {}", policySeq, url, e.getMessage());
        }
    }

    private String getKapacitorUrl(String influxdbUrl) {
        String influxUrl = influxdbUrl;
        String[] influxUrlParts = influxUrl.split(":");
        String influxDBPort = influxUrlParts[2];
        String kapacitorPort = "9092";
        String kapacitorUrl = influxUrl.replace(":" + influxDBPort, ":" + kapacitorPort);
        return kapacitorUrl;
    }

    private URI getKapacitorURI(String url) throws URISyntaxException {
        URI uri = null;

        try {
            uri = new URI(url);
            if (uri == null)
                throw new ResultCodeException(ResultCode.NOT_FOUND_DATA, "URI is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new URISyntaxException(uri.toString(), e.getMessage());
        }
        return uri;
    }
}
