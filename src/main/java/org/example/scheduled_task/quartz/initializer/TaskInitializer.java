package org.example.scheduled_task.quartz.initializer;

import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
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

    public final Integer EVERY_30_MINUTES = 30;

    public final Integer EVERY_40_MINUTES = 40;
    //每小时的第50分钟
    public final String AT_50TH_MINUTE_OF_EVERY_HOUR = "0 50 * * * ?";
    //每小时的第50分钟
    public final String AT_40TH_MINUTE_OF_EVERY_HOUR = "0 40 * * * ?";

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {

        //taskEventPublisher.publishTaskEvent(taskWithStrategy2, "refreshLatestPipelinesForProjectsCacheScheduledTask");
    }
}
