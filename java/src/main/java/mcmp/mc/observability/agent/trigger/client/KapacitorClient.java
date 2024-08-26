package mcmp.mc.observability.agent.trigger.client;

import mcmp.mc.observability.agent.trigger.model.KapacitorTaskInfo;
import mcmp.mc.observability.agent.trigger.model.KapacitorTaskListInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@FeignClient(name = "kapacitor")
public interface KapacitorClient {

    @GetMapping("/kapacitor/v1/tasks/{taskId}")
    KapacitorTaskInfo getTask(URI kapacitorURI, @PathVariable String taskId);

    @GetMapping("/kapacitor/v1/tasks")
    KapacitorTaskListInfo getTaskList(URI kapacitorURI);

    @PostMapping("/kapacitor/v1/tasks")
    void createTask(URI kapacitorURI, @RequestBody KapacitorTaskInfo kapacitorTaskInfo);

    @PatchMapping("/kapacitor/v1/tasks/{taskId}")
    void updateTask(URI kapacitorURI, @PathVariable String taskId, @RequestBody Map<String, String> status);

    @DeleteMapping("/kapacitor/v1/tasks/{taskId}")
    void deleteTask(URI kapacitorURI, @PathVariable String taskId);
}
