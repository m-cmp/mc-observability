package mcmp.mc.observability.agent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.trigger.client.KapacitorClient;
import mcmp.mc.observability.agent.trigger.model.KapacitorTaskInfo;
import mcmp.mc.observability.agent.trigger.model.KapacitorTaskListInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerTargetStorageInfo;
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

    public void createTask(TriggerPolicyInfo triggerPolicyInfo, TriggerTargetStorageInfo targetStorageInfo) {

        KapacitorTaskInfo kapacitorTaskInfo = new KapacitorTaskInfo();
        kapacitorTaskInfo.setId(String.valueOf(triggerPolicyInfo.getSeq()));
        // TODO: 사용자가 type 선택 할 수 있도록 변경
        kapacitorTaskInfo.setType("stream");
        kapacitorTaskInfo.setStatus(String.valueOf(triggerPolicyInfo.getIsEnabled()));
        kapacitorTaskInfo.setScript(triggerPolicyInfo.getTickScript());

        Map<String, String> dbrps = new HashMap<>();
        dbrps.put("db", targetStorageInfo.getDatabase());
        dbrps.put("rp", targetStorageInfo.getRetentionPolicy());
        kapacitorTaskInfo.setDbrps(Collections.singletonList(dbrps));

        String kapacitorUrl = getKapacitorUrl(targetStorageInfo.getUrl());

        try {
            KapacitorTaskInfo taskInfo = getTask(kapacitorUrl, kapacitorTaskInfo.getId());
            if(taskInfo != null)
                return;

            kapacitorClient.createTask(
                    getKapacitorURI(kapacitorUrl),
                    kapacitorTaskInfo
            );
        } catch (Exception e) {
            log.error("Failed to create Kapacitor task. TriggerPolicy Seq : {}, Storage URL : {}, Error: {}", triggerPolicyInfo.getSeq(), targetStorageInfo.getUrl(), e.getMessage());
        }
    }


    public void deleteTask(Long triggerSeq, String url) {

        String kapacitorUrl = getKapacitorUrl(url);
        String kapacitorTaskId = String.valueOf(triggerSeq);

        try {
            KapacitorTaskInfo taskInfo = getTask(kapacitorUrl, kapacitorTaskId);
            if(taskInfo == null)
                return;

            kapacitorClient.deleteTask(
                    getKapacitorURI(kapacitorUrl),
                    kapacitorTaskId
            );
        } catch (Exception e) {
            log.error("kapacitor task 삭제 실패! TriggerPolicy >> {}, kapacitorUrl >> {}", triggerSeq, url);
            throw new ResultCodeException(ResultCode.FAILED, "Failed to delete Trigger Task");
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
