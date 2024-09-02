package mcmp.mc.observability.mco11yagent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.model.InfluxDBConnector;
import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.mco11yagent.trigger.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerTaskManageService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerTargetMapper triggerTargetMapper;
    private final TriggerTargetStorageMapper triggerTargetStorageMapper;
    private final KapacitorApiService kapacitorApiService;

    public void manageTask() {
        List<ManageTriggerTargetStorageInfo> storageInfoList = triggerTargetStorageMapper.getManageTriggerTargetStorageInfoList(new HashMap<>());

        for(ManageTriggerTargetStorageInfo storageInfo : storageInfoList) {
            try {
                String url = storageInfo.getUrl();
                String database = storageInfo.getDatabase();
                String retentionPolicy = storageInfo.getRetentionPolicy();

                List<KapacitorTaskInfo> kapacitorTaskInfos = kapacitorApiService.getTaskList(url);
                List<Long> policySeqList = triggerTargetStorageMapper.getPolicySeqListRegisteredInStorage(Collections.singletonMap("url", url));

                if(!CollectionUtils.isEmpty(kapacitorTaskInfos) && !CollectionUtils.isEmpty(policySeqList)) {
                    List<Long> addPolicySeqList = new ArrayList<>(policySeqList);
                    addPolicySeqList.removeIf(a -> kapacitorTaskInfos.stream().anyMatch(b -> String.valueOf(a).equals(b.getId())));
                    kapacitorTaskInfos.removeIf(a -> policySeqList.stream().anyMatch(b -> String.valueOf(b).equals(a.getId())));
                    makeTriggerTask(addPolicySeqList, url, database, retentionPolicy);
                    removeTriggerTask(kapacitorTaskInfos, url);

                } else if (!CollectionUtils.isEmpty(kapacitorTaskInfos) && CollectionUtils.isEmpty(policySeqList)) {
                    removeTriggerTask(kapacitorTaskInfos, url);

                } else if (CollectionUtils.isEmpty(kapacitorTaskInfos) && !CollectionUtils.isEmpty(policySeqList)) {
                    makeTriggerTask(policySeqList, url, database, retentionPolicy);

                }
            } catch (Exception e) {
                log.error("Failed to sync kapacitor task. Error : {}, ", e.getMessage());
            }
        }
    }

    private void makeTriggerTask(List<Long> addPolicySeqist, String url, String database, String retentionPolicy) {
        if(!CollectionUtils.isEmpty(addPolicySeqist)) {
            for(Long policySeq : addPolicySeqist) {
                TriggerPolicyInfo triggerPolicyInfo = triggerPolicyMapper.getDetail(policySeq);
                try {
                    kapacitorApiService.createTask(triggerPolicyInfo, url, database, retentionPolicy);
                } catch (Exception e) {
                    log.error("Failed to make trigger task. PolicySeq : {}", triggerPolicyInfo.getSeq());
                }
            }
        }
    }

    private void removeTriggerTask(List<KapacitorTaskInfo> kapacitorTaskInfoList, String url) {
        if(!CollectionUtils.isEmpty(kapacitorTaskInfoList)) {
            for(KapacitorTaskInfo kapacitorTaskInfo : kapacitorTaskInfoList) {
                try {
                    kapacitorApiService.deleteTask(Long.valueOf(kapacitorTaskInfo.getId()), url);
                } catch (Exception e) {
                    log.error("Failed to remove trigger task : Task Id : {}", kapacitorTaskInfo.getId());
                }
            }
        }
    }

    public void manageStorage(MonitoringConfigInfo originalInfo, MonitoringConfigInfo newInfo) {
        Map<String, Object> params = new HashMap<>();
        params.put("targetId", newInfo.getTargetId());
        params.put("nsId", newInfo.getNsId());
        List<TriggerTargetInfo> triggerTargetInfoList = triggerTargetMapper.getTargetList(params);
        if(CollectionUtils.isEmpty(triggerTargetInfoList))
            return;

        InfluxDBConnector originalInfluxdbInfo = new InfluxDBConnector(originalInfo.getPluginConfig());
        InfluxDBConnector newInfluxdbInfo = new InfluxDBConnector(newInfo.getPluginConfig());

        for(TriggerTargetInfo targetInfo : triggerTargetInfoList) {
            params.put("targetSeq", targetInfo.getSeq());
            params.put("url", originalInfluxdbInfo.getUrl());
            params.put("database", originalInfluxdbInfo.getDatabase());
            params.put("retentionPolicy", originalInfluxdbInfo.getRetentionPolicy());

            TriggerTargetStorageInfo originTriggerStorageInfo = triggerTargetStorageMapper.getStorageInfo(params);

            // The number of storages with the policy set on the target.
            params.put("policySeq", targetInfo.getPolicySeq());
            Long usageCount = triggerTargetStorageMapper.getUsageStorageCount(params);
            if(usageCount < 2) {
                // task delete in origin storage
                kapacitorApiService.deleteTask(targetInfo.getPolicySeq(), originalInfluxdbInfo.getUrl());
            }

            // Check if the agent storage information is registered in the target storage.
            // Insert if not registered, update if already exists.
            if(originTriggerStorageInfo == null) {
                TriggerTargetStorageInfo storageInfo = TriggerTargetStorageInfo.builder()
                        .targetSeq(targetInfo.getSeq())
                        .url(newInfluxdbInfo.getUrl())
                        .retentionPolicy(newInfluxdbInfo.getRetentionPolicy())
                        .database(newInfluxdbInfo.getDatabase())
                        .build();
                triggerTargetStorageMapper.createTargetStorage(storageInfo);
            } else {
                originTriggerStorageInfo.updateInfluxDBConfig(newInfluxdbInfo);
                triggerTargetStorageMapper.updateTargetStorage(originTriggerStorageInfo);
            }

            // Create a task for the modified storage
            TriggerPolicyInfo policyInfo = triggerPolicyMapper.getDetail(targetInfo.getPolicySeq());
            kapacitorApiService.createTask(policyInfo, newInfluxdbInfo.getUrl(), newInfluxdbInfo.getDatabase(), newInfluxdbInfo.getRetentionPolicy());
        }
    }
}
