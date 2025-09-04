package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.model.semaphore.Inventory;
import com.mcmp.o11ymanager.manager.model.semaphore.LoginRequest;
import com.mcmp.o11ymanager.manager.model.semaphore.Project;
import com.mcmp.o11ymanager.manager.model.semaphore.Repository;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.model.semaphore.Template;
import java.util.List;

public interface SemaphorePort {
    void login(LoginRequest loginRequest);

    Task createTask(Integer projectId, Task task);

    Task getTask(Integer projectId, Integer taskId);

    List<Project> getProjects();

    Project getProjectByName(String name);

    List<Template> getTemplates(Integer projectId);

    Project createProject(Project project);

    List<Inventory> getInventories(Integer projectId);

    Inventory createInventory(Integer projectId, Inventory inventory);

    List<Repository> getRepositories(Integer projectId);

    Repository createRepository(Integer projectId, Repository repository);

    Template createTemplate(Integer projectId, Template template);
}
