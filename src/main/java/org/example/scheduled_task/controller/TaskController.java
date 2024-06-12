package org.example.scheduled_task.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.quartz.ScheduledTaskRegistry;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
import org.example.scheduled_task.quartz.strategy.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.TestTask;
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
    private TaskEventPublisher taskEventPublisher;
    @Resource
    private ScheduledTaskRegistry scheduledTaskRegistry;

    /**
     * 添加任务
     *
     * @param cronExpression
     * @param taskId
     * @param taskName
     * @return
     */
    @GetMapping("/add")
    public String addTask(@RequestParam("cronExpression") String cronExpression,
                          @RequestParam("taskId") String taskId,
                          @RequestParam("taskName") String taskName) {

        CronScheduleStrategy cronScheduleStrategy = new CronScheduleStrategy(cronExpression);

        TestTask testTask = new TestTask();

        ScheduledTaskMetaData<Void> scheduledTaskMetaData =
                new ScheduledTaskMetaData<>(taskId, taskName, cronScheduleStrategy, testTask);

        if (!scheduledTaskRegistry.containsTask(taskId)) {

            scheduledTaskRegistry.registerTask(scheduledTaskMetaData.getTaskId(), scheduledTaskMetaData);

            taskEventPublisher.publishTaskEvent(scheduledTaskMetaData);
        }
        log.info("scheduledTaskMetaData 对象:{}", scheduledTaskMetaData.toString());

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
        log.info("taskId:{}", taskId);
        try {
            ScheduledTaskMetaData<?> taskMetaData = scheduledTaskRegistry.getScheduledTaskMetaData(taskId);
            if (taskMetaData != null) {
                // 当调用 executeTask 方法时，如果任务之前被取消，那么你需要重置任务的状态以便它可以被重新调度和执行
                if (scheduledTaskRegistry.isCanceled(taskId)) {
                    // 重制任务状态已添加
                    scheduledTaskRegistry.resetCanceled(taskId);
                }
                //房补事件
                taskEventPublisher.publishTaskEvent(taskMetaData);
            }
        } catch (Exception e) {
            return "Error executing task: " + e.getMessage();
        }

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
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.cancelTask(taskId);
            }
        } catch (Exception e) {
            return "Error cancelling task: " + e.getMessage();
        }
        return "cancel";
    }

    // 删除任务元信息
    @GetMapping("/delete")
    public String deleteTask(@RequestParam("taskId") String taskId) {
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.deleteTask(taskId);
            }
        } catch (Exception e) {
            return "Error deleting task: " + e.getMessage();
        }
        return "delete";
    }
}
