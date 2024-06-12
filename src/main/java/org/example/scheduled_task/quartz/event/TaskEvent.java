package org.example.scheduled_task.quartz.event;


import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.springframework.context.ApplicationEvent;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/05/24 10:33
 */

public class TaskEvent extends ApplicationEvent {

    private ScheduledTaskMetaData<?> scheduledTaskMetaData;

    private String taskName;

    public TaskEvent(Object source, ScheduledTaskMetaData<?> scheduledTaskMetaData, String taskName) {
        super(source);
        this.scheduledTaskMetaData = scheduledTaskMetaData;
        this.taskName = taskName;
    }

    public ScheduledTaskMetaData<?> getScheduledTaskWithStrategy() {
        return scheduledTaskMetaData;
    }

    public void setScheduledTaskWithStrategy(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        this.scheduledTaskMetaData = scheduledTaskMetaData;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
}
