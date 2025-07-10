package com.innogrid.tabcloudit.o11ymanager.port;

import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Inventory;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.LoginRequest;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Project;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Repository;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Task;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Template;
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