package org.example.scheduled_task.service;

import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/12 13:40
 */
public interface TaskService {

    void addTask(ScheduledTaskMetaData<?> scheduledTaskMetaData);

    void deleteTask(String taskId);

    void executeTask(String taskId);

    void cancelTask(String taskId);
}
