package com.mcmp.o11ymanager.manager.infrastructure.port.semaphore;

import com.mcmp.o11ymanager.manager.model.semaphore.Inventory;
import com.mcmp.o11ymanager.manager.model.semaphore.LoginRequest;
import com.mcmp.o11ymanager.manager.model.semaphore.Project;
import com.mcmp.o11ymanager.manager.model.semaphore.Repository;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.model.semaphore.Template;
import java.util.List;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "semaphore",
        url = "${feign.semaphore.url:}",
        configuration = SemaphoreFeignConfig.class)
public interface SemaphoreClient {
    // Auth
    @PostMapping("/api/auth/login")
    void login(@RequestBody LoginRequest request);

    // Project
    @PostMapping("/api/projects")
    Project createProject(@RequestBody Project request);

    @GetMapping("/api/projects")
    Optional<List<Project>> getProjects();

    @PostMapping("/api/project/{projectId}/inventory")
    Inventory createInventory(
            @PathVariable("projectId") int projectId, @RequestBody Inventory request);

    @GetMapping("/api/project/{projectId}/inventory")
    Optional<List<Inventory>> getInventories(@PathVariable("projectId") int projectId);

    @PostMapping("/api/project/{projectId}/repositories")
    Repository createRepository(
            @PathVariable("projectId") int projectId, @RequestBody Repository request);

    @GetMapping("/api/project/{projectId}/repositories")
    Optional<List<Repository>> getRepositories(@PathVariable("projectId") int projectId);

    @PostMapping("/api/project/{projectId}/templates")
    Template createTemplate(
            @PathVariable("projectId") int projectId, @RequestBody Template request);

    @GetMapping("/api/project/{projectId}/templates")
    List<Template> getTemplates(@PathVariable("projectId") int projectId);

    @GetMapping("/api/project/{projectId}/tasks/{taskId}")
    Task getTask(@PathVariable("projectId") int projectId, @PathVariable("taskId") int taskId);

    @PostMapping("/api/project/{projectId}/tasks")
    Task createTask(@PathVariable("projectId") int projectId, @RequestBody Task request);
}
