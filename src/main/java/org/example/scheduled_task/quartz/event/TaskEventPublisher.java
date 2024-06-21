package org.example.scheduled_task.quartz.event;


import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/05/24 10:38
 */
@Component
public class TaskEventPublisher {
    // spring的事件发布器
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishTaskEvent(ScheduledTaskMetaData<?> scheduledTaskMetaData) {

        TaskEvent taskEvent = new TaskEvent(this, scheduledTaskMetaData);

        applicationEventPublisher.publishEvent(taskEvent);
    }
}
