package org.example.scheduled_task.service;

import jakarta.annotation.Resource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.quartz.ScheduledTaskRegistry;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/12 13:43
 */
@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Resource
    private TaskEventPublisher taskEventPublisher;
    @Resource
    private ScheduledTaskRegistry scheduledTaskRegistry;

    @Override
    public void addTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        if (!scheduledTaskRegistry.containsTask(scheduledTaskMetaData.getTaskId())) {

            scheduledTaskRegistry.registerTask(scheduledTaskMetaData.getTaskId(), scheduledTaskMetaData);
        }
        log.info("");
    }

    @Override
    public void deleteTask(String taskId) {
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.deleteTask(taskId);
            }
        } catch (Exception e) {
            // return "Error deleting task: " + e.getMessage();
        }
    }

    @Override
    public void executeTask(String taskId) {
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
            //return "Error executing task: " + e.getMessage();
        }
    }

    @Override
    public void cancelTask(String taskId) {
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.cancelTask(taskId);
            }
        } catch (Exception e) {
            // return "Error cancelling task: " + e.getMessage();
        }
    }
}
