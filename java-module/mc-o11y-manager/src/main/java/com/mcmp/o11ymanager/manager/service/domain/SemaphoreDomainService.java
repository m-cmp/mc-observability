package com.mcmp.o11ymanager.manager.service.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.exception.agent.SemaphoreException;
import com.mcmp.o11ymanager.manager.facade.FileFacadeService;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.global.definition.ConfigDefinition;
import com.mcmp.o11ymanager.manager.model.semaphore.Environment;
import com.mcmp.o11ymanager.manager.model.semaphore.Inventory;
import com.mcmp.o11ymanager.manager.model.semaphore.LoginRequest;
import com.mcmp.o11ymanager.manager.model.semaphore.Project;
import com.mcmp.o11ymanager.manager.model.semaphore.Repository;
import com.mcmp.o11ymanager.manager.model.semaphore.SurveyVar;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.model.semaphore.Template;
import com.mcmp.o11ymanager.manager.port.SemaphorePort;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemaphoreDomainService {

    public static final int SEMAPHORE_MAX_PARALLEL_TASKS = 10;

    private final SemaphorePort semaphorePort;
    private final FileFacadeService fileFacadeService;
    private final RequestInfo requestInfo;

    @Value("${deploy.site-code}")
    private String deploySiteCode;

    @Value("${feign.semaphore.project-name}")
    private String projectName;

    @Value("${feign.semaphore.template-names.agent-install}")
    private String templateNameAgentInstall;

    @Value("${feign.semaphore.template-names.agent-config-update}")
    private String templateNameAgentConfigUpdate;

    @Value("${feign.semaphore.username}")
    private String username;

    @Value("${feign.semaphore.password}")
    private String password;

    public Task install(
            AccessInfoDTO accessInfo,
            SemaphoreInstallMethod method,
            String configContent,
            Agent agent,
            int templateCount) {

        log.info("======================START SEMAPHORE INSTALL==============================");

        String methodStr = Strings.toLowerCase(method.toString());

        try {
            log.info("📌 step1: create env");
            Environment env =
                    createEnvironment(
                            agent,
                            accessInfo.getIp(),
                            accessInfo.getPort(),
                            accessInfo.getUser(),
                            accessInfo.getSshKey());

            env.addVariable("install_method", methodStr);
            env.addVariable(
                    "telegraf_config_path",
                    fileFacadeService.getHostConfigTelegrafRemotePath()
                            + "/"
                            + ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG);
            env.addVariable(
                    "fluentbit_config_path",
                    fileFacadeService.getHostConfigFluentBitRemotePath()
                            + "/"
                            + ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES);
            if (method == SemaphoreInstallMethod.INSTALL && configContent != null) {
                env.addVariable(
                        "config_content",
                        Base64.getEncoder().encodeToString(configContent.getBytes()));
            }

            log.info("📌 step2: get project");
            Project project = getProjectByName(projectName);
            log.info(
                    "========================projectName={}============================",
                    project.getName());

            String templateName = templateNameAgentInstall + "_" + templateCount;

            log.info(
                    "========================templateName={}============================",
                    templateName);

            log.info("📌 step3: get template");
            Template template =
                    getTemplates(project.getId()).stream()
                            .filter(i -> i.getName().equals(templateName))
                            .findFirst()
                            .orElseThrow(NoSuchElementException::new);

            log.info("📌 step4: create task");
            Task task = createTask(template.getId(), env);

            log.info("📌 step5: call semaphorePort.createTask()");
            return semaphorePort.createTask(project.getId(), task);
        } catch (Exception e) {
            throw new SemaphoreException("Failed to " + methodStr + " agent", e);
        }
    }

    public Task updateConfig(
            AccessInfoDTO accessInfo,
            String configPath,
            String configContent,
            Agent agent,
            int templateCount) {
        try {
            Environment env =
                    createEnvironment(
                            agent,
                            accessInfo.getIp(),
                            accessInfo.getPort(),
                            accessInfo.getUser(),
                            accessInfo.getSshKey());

            env.addVariable("config_path", configPath);
            env.addVariable(
                    "config_content", Base64.getEncoder().encodeToString(configContent.getBytes()));

            Project project = getProjectByName(projectName);
            String templateName = templateNameAgentConfigUpdate + "_" + templateCount;

            Template template =
                    getTemplates(project.getId()).stream()
                            .filter(i -> i.getName().equals(templateName))
                            .findFirst()
                            .orElseThrow(NoSuchElementException::new);

            Task task = createTask(template.getId(), env);
            return semaphorePort.createTask(project.getId(), task);

        } catch (Exception e) {
            throw new SemaphoreException("Failed to update configuration", e);
        }
    }

    private Environment createEnvironment(
            Agent agent, String ip, int port, String user, String sshkey) {
        return new Environment()
                .addVariable("agent", agent.getName().toLowerCase())
                .addVariable("site_code", deploySiteCode)
                .addVariable("request_id", requestInfo.getRequestId())
                .addVariable("target_host", ip)
                .addVariable("vm_port", String.valueOf(port))
                .addVariable("vm_user", user)
                .addVariable("vm_sshkey", Base64.getEncoder().encodeToString(sshkey.getBytes()));
    }

    private Task createTask(Integer templateId, Environment environment)
            throws JsonProcessingException {
        return Task.builder().templateId(templateId).build().setEnvironmentString(environment);
    }

    private void login() {
        LoginRequest loginRequest = new LoginRequest(username, password);
        semaphorePort.login(loginRequest);
    }

    private List<Project> getProjects() {
        login();
        return semaphorePort.getProjects();
    }

    public Project getProjectByName(String name) {
        return getProjects().stream()
                .filter(i -> i.getName().equals(name))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    private List<Template> getTemplates(Integer projectId) {
        login();
        return semaphorePort.getTemplates(projectId);
    }

    public void initProject(String projectName) {
        try {
            login();

            Project project =
                    Project.builder()
                            .name(projectName)
                            .alert(true)
                            .maxParallelTasks(SEMAPHORE_MAX_PARALLEL_TASKS)
                            .build();

            semaphorePort.createProject(project);
        } catch (Exception e) {
            throw new RuntimeException("프로젝트 생성 중 오류가 발생 하였습니다 😵💫", e);
        }
    }

    public Inventory getInventoryByName(String projectName, String inventoryName) {
        Project project = getProjectByName(projectName);

        List<Inventory> inventories = semaphorePort.getInventories(project.getId());

        return inventories.stream()
                .filter(i -> i.getName().equals(inventoryName))
                .findFirst()
                .orElseThrow();
    }

    public void initProjectInventory(String projectName, String inventoryName) {
        try {
            Project project = getProjectByName(projectName);

            Inventory request = new Inventory();

            request.setProjectId(project.getId());
            request.setName(inventoryName);
            request.setSshKeyId(1);
            request.setType("static");
            request.setInventory("[all]\n127.0.0.1 ansible_connection=local");

            semaphorePort.createInventory(project.getId(), request);
        } catch (Exception e) {
            throw new RuntimeException("인벤토리 생성 중 오류가 발생 하였습니다 😵💫");
        }
    }

    public Repository getProjectRepositoryId(String projectName, String repositoryName) {
        Project project = getProjectByName(projectName);
        List<Repository> repositories = semaphorePort.getRepositories(project.getId());
        return repositories.stream()
                .filter(i -> i.getName().equals(repositoryName))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    public void initProjectRepository(String projectName, String repositoryName, String gitUrl) {
        try {
            Project project = getProjectByName(projectName);

            Repository repository =
                    Repository.builder()
                            .projectId(project.getId())
                            .name(repositoryName)
                            .gitUrl(gitUrl)
                            .sshKeyId(1)
                            .build();

            semaphorePort.createRepository(project.getId(), repository);
        } catch (Exception e) {
            throw new RuntimeException(repositoryName + " 레포지토리 생성 중 오류가 발생 하였습니다 😵💫");
        }
    }

    public void checkProjectTemplate(String projectName, String templateName) {
        Project project = getProjectByName(projectName);

        List<Template> templates = semaphorePort.getTemplates(project.getId());

        templates.stream()
                .filter(i -> i.getName().equals(templateName))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    public void initProjectTemplate(
            String projectName,
            String inventoryName,
            String templateName,
            String playbookName,
            List<SurveyVar> surveyVars) {

        Project project = getProjectByName(projectName);
        Inventory inventory = getInventoryByName(projectName, inventoryName);
        Repository repository = getProjectRepositoryId(projectName, templateName);

        for (int i = 0; i < SEMAPHORE_MAX_PARALLEL_TASKS; i++) {
            try {
                checkProjectTemplate(projectName, templateName + "_" + (i + 1));
            } catch (NoSuchElementException e) {
                Template template =
                        Template.builder()
                                .projectId(project.getId())
                                .inventoryId(inventory.getId())
                                .repositoryId(repository.getId())
                                .environmentId(1)
                                .name(templateName + "_" + (i + 1))
                                .playbook(playbookName)
                                .arguments("[]")
                                .type("")
                                .app("ansible")
                                .surveyVars(surveyVars)
                                .build();

                semaphorePort.createTemplate(project.getId(), template);
            } catch (Exception e) {
                throw new RuntimeException(
                        templateNameAgentInstall + " 템플릿 등록 중 오류가 발생 하였습니다 😵💫");
            }
        }
    }
}
