package com.mcmp.o11ymanager.infrastructure.port.semaphore;

import com.mcmp.o11ymanager.model.semaphore.Inventory;
import com.mcmp.o11ymanager.model.semaphore.LoginRequest;
import com.mcmp.o11ymanager.model.semaphore.Project;
import com.mcmp.o11ymanager.model.semaphore.Repository;
import com.mcmp.o11ymanager.model.semaphore.Task;
import com.mcmp.o11ymanager.model.semaphore.Template;
import com.mcmp.o11ymanager.port.SemaphorePort;
import java.util.List;
import java.util.NoSuchElementException;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SemaphoreAdapter implements SemaphorePort {
    private final SemaphoreClient semaphoreClient;

    @Override
    public void login(LoginRequest loginRequest) {
        try {
            semaphoreClient.login(loginRequest);
            log.info("=======================Semaphore Login Success==============================");
        } catch (Exception e) {
            log.error("[Semaphore] Login failed for request: {}", loginRequest, e);
            throw e;
        }
    }


    @Override
    public Task createTask(Integer projectId, Task task) {
        return semaphoreClient.createTask(projectId, task);
    }

    @Override
    public Task getTask(Integer projectId, Integer taskId) {
        return semaphoreClient.getTask(projectId, taskId);
    }

    @Override
    public List<Project> getProjects() {
        try {
            Optional<List<Project>> result = semaphoreClient.getProjects();

            if (result.isEmpty()) {
                log.error("[Semaphore] getProjects() failed: empty result");
                throw new IllegalStateException("No projects returned from Semaphore API");
            }

            log.info("[Semaphore] getProjects() success: {} items", result.get().size());
            return result.get();
        } catch (Exception e) {
            log.error("[Semaphore] Exception while calling getProjects()", e);
            throw e;
        }
    }

    @Override
    public Project getProjectByName(String name) {
        return getProjects().stream().filter(i -> i.getName().equals(name)).findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    @Override
    public List<Template> getTemplates(Integer projectId) {
        return semaphoreClient.getTemplates(projectId);
    }

    @Override
    public Project createProject(Project project) {
        return semaphoreClient.createProject(project);
    }

    @Override
    public List<Inventory> getInventories(Integer id) {
        return semaphoreClient.getInventories(id).orElseThrow();
    }

    @Override
    public Inventory createInventory(Integer projectId, Inventory inventory) {
        return semaphoreClient.createInventory(projectId, inventory);
    }

    @Override
    public List<Repository> getRepositories(Integer projectId) {
        return semaphoreClient.getRepositories(projectId).orElseThrow();
    }

    @Override
    public Repository createRepository(Integer projectId, Repository repository) {
        return semaphoreClient.createRepository(projectId, repository);
    }

    @Override
    public Template createTemplate(Integer projectId, Template template) {
        return semaphoreClient.createTemplate(projectId, template);
    }

} 