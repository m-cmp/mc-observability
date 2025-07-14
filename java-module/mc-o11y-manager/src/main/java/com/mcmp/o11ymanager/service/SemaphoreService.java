package com.mcmp.o11ymanager.service;


import com.mcmp.o11ymanager.model.semaphore.SurveyVar;

import java.util.*;

import com.mcmp.o11ymanager.service.domain.SemaphoreDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemaphoreService {

    private final SemaphoreDomainService semaphoreDomainService;

    @Value("${feign.semaphore.template-names.agent-install}")
    private String templateNameAgentInstall;

    @Value("${feign.semaphore.template-names.agent-config-update}")
    private String templateNameAgentConfigUpdate;

    private static final String PLAYBOOK_ROOT_PATH = "/ansible/playbooks/";
    private static final String PLAYBOOK_FILE_NAME = "playbook.yaml";

    @Value("${feign.semaphore.project-name}")
    private String projectName;

    public void initSemaphore() {
        String inventoryName = "all";

        // 1. init project
        try {
            semaphoreDomainService.getProjectByName(projectName);
        } catch (NoSuchElementException e) {
            log.info("프로젝트 생성중... ⚙️");
            semaphoreDomainService.initProject(projectName);
        }

        // 2. init inventory
        try {
            semaphoreDomainService.getInventoryByName(projectName, inventoryName);
        } catch (NoSuchElementException e) {
            log.info("인벤토리 생성중... ⚙️");
            semaphoreDomainService.initProjectInventory(projectName, inventoryName);
        }

        // 3. init repository
        try {
            semaphoreDomainService.getProjectRepositoryId(projectName, templateNameAgentInstall);
        } catch (NoSuchElementException e) {
            log.info(templateNameAgentInstall + " 레포지토리 생성중... ⚙️");
            semaphoreDomainService.initProjectRepository(projectName, templateNameAgentInstall,
                    PLAYBOOK_ROOT_PATH + "/" + templateNameAgentInstall);
        }

        try {
            semaphoreDomainService.getProjectRepositoryId(projectName, templateNameAgentConfigUpdate);
        } catch (NoSuchElementException e) {
            log.info(templateNameAgentConfigUpdate + " 레포지토리 생성중... ⚙️");
            semaphoreDomainService.initProjectRepository(projectName, templateNameAgentConfigUpdate,
                    PLAYBOOK_ROOT_PATH + "/" + templateNameAgentConfigUpdate);
        }

        // 4. init project template
        try {
            for (int i = 0; i < SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS; i++) {
                semaphoreDomainService.checkProjectTemplate(projectName, templateNameAgentInstall + "_" + (i + 1));
            }
        } catch (NoSuchElementException e) {
            log.info("{} 템플릿 생성중... ⚙️", templateNameAgentInstall);
            List<SurveyVar> surveyVars = createAgentInstallTemplate();
            semaphoreDomainService.initProjectTemplate(projectName,
                    inventoryName,
                    templateNameAgentInstall,
                    PLAYBOOK_FILE_NAME,
                    surveyVars);
        }

        try {
            for (int i = 0; i < SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS; i++) {
                semaphoreDomainService.checkProjectTemplate(projectName, templateNameAgentConfigUpdate + "_" + (i + 1));
            }
        } catch (NoSuchElementException e) {
            log.info("{} 템플릿 생성중... ⚙️", templateNameAgentConfigUpdate);
            List<SurveyVar> surveyVars = createAgentConfigUpdateTemplate();
            semaphoreDomainService.initProjectTemplate(projectName,
                    inventoryName,
                    templateNameAgentConfigUpdate,
                    PLAYBOOK_FILE_NAME,
                    surveyVars);
        }
    }

    private List<SurveyVar> createTemplateCommon() {
        List<SurveyVar> surveyVars = new ArrayList<>();

        SurveyVar agent = new SurveyVar();
        agent.setValues(new ArrayList<>());
        agent.setName("agent");
        agent.setTitle("agent");
        agent.setDescription("");
        agent.setType("");
        agent.setRequired(true);
        surveyVars.add(agent);

        SurveyVar siteCode = new SurveyVar();
        siteCode.setValues(new ArrayList<>());
        siteCode.setName("site_code");
        siteCode.setTitle("site_code");
        siteCode.setDescription("");
        siteCode.setType("");
        siteCode.setRequired(true);
        surveyVars.add(siteCode);

        SurveyVar requestId = new SurveyVar();
        requestId.setValues(new ArrayList<>());
        requestId.setName("request_id");
        requestId.setTitle("request_id");
        requestId.setDescription("");
        requestId.setType("");
        requestId.setRequired(true);
        surveyVars.add(requestId);

        SurveyVar targetHost = new SurveyVar();
        targetHost.setValues(new ArrayList<>());
        targetHost.setName("target_host");
        targetHost.setTitle("target_host");
        targetHost.setDescription("");
        targetHost.setType("");
        targetHost.setRequired(true);
        surveyVars.add(targetHost);

        SurveyVar targetPort = new SurveyVar();
        targetPort.setValues(new ArrayList<>());
        targetPort.setName("target_port");
        targetPort.setTitle("target_port");
        targetPort.setDescription("");
        targetPort.setType("");
        targetPort.setRequired(true);
        surveyVars.add(targetPort);

        SurveyVar targetUser = new SurveyVar();
        targetUser.setValues(new ArrayList<>());
        targetUser.setName("target_user");
        targetUser.setTitle("target_user");
        targetUser.setDescription("");
        targetUser.setType("");
        targetUser.setRequired(true);
        surveyVars.add(targetUser);

        SurveyVar targetPassword = new SurveyVar();
        targetPassword.setValues(new ArrayList<>());
        targetPassword.setName("target_password");
        targetPassword.setTitle("target_password");
        targetPassword.setDescription("");
        targetPassword.setType("");
        targetPassword.setRequired(true);
        surveyVars.add(targetPassword);

        SurveyVar configContent = new SurveyVar();
        configContent.setValues(new ArrayList<>());
        configContent.setName("config_content");
        configContent.setTitle("config_content");
        configContent.setDescription("");
        configContent.setType("");
        configContent.setRequired(true);
        surveyVars.add(configContent);

        SurveyVar telegrafConfigPath = new SurveyVar();
        telegrafConfigPath.setValues(new ArrayList<>());
        telegrafConfigPath.setName("telegraf_config_path");
        telegrafConfigPath.setTitle("telegraf_config_path");
        telegrafConfigPath.setDescription("");
        telegrafConfigPath.setType("");
        telegrafConfigPath.setRequired(true);
        surveyVars.add(telegrafConfigPath);

        SurveyVar fluentBitConfigPath = new SurveyVar();
        fluentBitConfigPath.setValues(new ArrayList<>());
        fluentBitConfigPath.setName("fluentbit_config_path");
        fluentBitConfigPath.setTitle("fluentbit_config_path");
        fluentBitConfigPath.setDescription("");
        fluentBitConfigPath.setType("");
        fluentBitConfigPath.setRequired(true);
        surveyVars.add(fluentBitConfigPath);

        return surveyVars;
    }

    private List<SurveyVar> createAgentInstallTemplate() {
        List<SurveyVar> surveyVars = createTemplateCommon();

        SurveyVar installMethod = new SurveyVar();
        installMethod.setValues(new ArrayList<>());
        installMethod.setName("install_method");
        installMethod.setTitle("install_method");
        installMethod.setDescription("");
        installMethod.setType("");
        installMethod.setRequired(true);
        surveyVars.add(installMethod);

        return surveyVars;
    }

    private List<SurveyVar> createAgentConfigUpdateTemplate() {
        List<SurveyVar> surveyVars = createTemplateCommon();

        SurveyVar configPath = new SurveyVar();
        configPath.setValues(new ArrayList<>());
        configPath.setName("config_path");
        configPath.setTitle("config_path");
        configPath.setDescription("");
        configPath.setType("");
        configPath.setRequired(true);
        surveyVars.add(configPath);

        return surveyVars;
    }
}
