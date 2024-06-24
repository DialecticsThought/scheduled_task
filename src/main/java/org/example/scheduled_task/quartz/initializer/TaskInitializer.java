package org.example.scheduled_task.quartz.initializer;

import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
import org.example.scheduled_task.quartz.registry.DatabaseTaskRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author jiahao.liu
 * @description 这个类 就是用来在项目启动的时候，就加载在onApplicationReady方法中定义的任务
 * @date 2024/05/24 10:53
 */
@Component
public class TaskInitializer {
    @Resource
    private TaskEventPublisher taskEventPublisher;
    @Resource
    private DatabaseTaskRegistry databaseTaskRegistry;

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        //databaseTaskRegistry.initializeTasksFromDatabase();
    }
}
