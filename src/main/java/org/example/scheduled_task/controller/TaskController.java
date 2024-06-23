package org.example.scheduled_task.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.entity.CoarseScheduledTaskMetaData;
import org.example.scheduled_task.service.TaskService;
import org.springframework.web.bind.annotation.*;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/11 19:09
 */
@RestController
@RequestMapping("/task")
@Slf4j
public class TaskController {
    @Resource
    private TaskService taskService;

    /**
     * 添加任务
     *
     * @return
     */
    @PostMapping("/add")
    public String addTask(@RequestBody CoarseScheduledTaskMetaData coarseScheduledTaskMetaData) {
        taskService.addTaskCompletely(coarseScheduledTaskMetaData);
        return "add";
    }

    /**
     * 执行任务
     *
     * @param taskId
     * @return
     */
    @GetMapping("/execute")
    public String executeTask(@RequestParam("taskId") String taskId) {
        taskService.executeTask(taskId);
        return "execute";
    }

    /**
     * 取消任务，但保留元信息
     *
     * @param taskId
     * @return
     */
    @GetMapping("/cancel")
    public String cancelTask(@RequestParam("taskId") String taskId) {
        taskService.cancelTask(taskId);
        return "cancel";
    }

    /**
     * 删除任务元信息
     *
     * @param taskId
     * @return
     */
    @GetMapping("/delete")
    public String deleteTask(@RequestParam("taskId") String taskId) {
        taskService.deleteTask(taskId);
        return "delete";
    }
}
