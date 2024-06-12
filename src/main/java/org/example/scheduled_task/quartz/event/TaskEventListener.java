package org.example.scheduled_task.quartz.event;


import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.ScheduledTaskRegistry;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.factory.QuartzJobFactory;
import org.example.scheduled_task.quartz.strategy.ScheduleStrategy;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/05/24 10:40
 */
@Component
public class TaskEventListener {

    @Resource
    private ScheduledTaskRegistry scheduledTaskRegistry;
    //QuartzConfig注册的  必须要用Autowired
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    /**
     * 把任务祖册到Quartz调度器里面
     * @param taskName
     * @param scheduleStrategy
     */
    private void scheduleJob(String taskName, ScheduleStrategy scheduleStrategy) {
        ScheduledTaskMetaData<?> taskWithStrategy = scheduledTaskRegistry.getTaskWithStrategy(taskName);

        JobDetail jobDetail = JobBuilder.newJob(QuartzJobFactory.class).withIdentity(taskName).build();

        Trigger trigger = scheduleStrategy.getTrigger(taskName);

        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @EventListener
    public void handleTaskEvent(TaskEvent taskEvent) {
        String taskName = taskEvent.getTaskName();

        ScheduledTaskMetaData<?> scheduledTaskMetaData = taskEvent.getScheduledTaskWithStrategy();

        scheduledTaskRegistry.registerTask(taskName, scheduledTaskMetaData);

        scheduleJob(taskName, scheduledTaskMetaData.getScheduleStrategy());

    }
}
