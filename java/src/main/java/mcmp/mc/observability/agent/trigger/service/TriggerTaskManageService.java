package mcmp.mc.observability.agent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.agent.trigger.model.KapacitorTaskInfo;
import mcmp.mc.observability.agent.trigger.model.ManageTriggerTargetStorageInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerTaskManageService {

    private final TriggerPolicyMapper triggerPolicyMapper;
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
}
