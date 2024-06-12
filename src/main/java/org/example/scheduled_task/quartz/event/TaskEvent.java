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

    public TaskEvent(Object source, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        super(source);
        this.scheduledTaskMetaData = scheduledTaskMetaData;
    }

    public ScheduledTaskMetaData<?> getScheduledTaskMetaData() {
        return scheduledTaskMetaData;
    }

    public void setScheduledTaskMetaData(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        this.scheduledTaskMetaData = scheduledTaskMetaData;
    }
}
