package org.example.scheduled_task.quartz.event;


import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.factory.QuartzJobFactory;
import org.example.scheduled_task.quartz.registry.DatabaseTaskRegistry;
import org.example.scheduled_task.quartz.registry.EnhancedRedisScheduledTaskRegistry;
import org.example.scheduled_task.quartz.registry.ScheduledTaskRegistry;
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
    // 服务注册中心
    @Resource
    private DatabaseTaskRegistry scheduledTaskRegistry;
    // QuartzConfig注册的,必须要用Autowired
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @EventListener
    public void handleTaskEvent(TaskEvent taskEvent) {

        ScheduledTaskMetaData<?> scheduledTaskMetaData = taskEvent.getScheduledTaskMetaData();
        // 注册中心是否有这个任务，没有这个任务不能执行
        if (scheduledTaskRegistry.containsTask(scheduledTaskMetaData.getTaskId())) {
            //scheduledTaskRegistry.registerTask(scheduledTaskMetaData);
            //TODO 核心
            scheduleJob(scheduledTaskMetaData);

        }
    }

    /**
     * 把任务注册到Quartz调度器里面
     */
    private void scheduleJob(ScheduledTaskMetaData<?> scheduledTaskMetaData) {

        JobDetail jobDetail = JobBuilder.newJob(QuartzJobFactory.class).withIdentity(scheduledTaskMetaData.getTaskId()).build();

        Trigger trigger = scheduledTaskMetaData.getScheduleStrategy().getTrigger(scheduledTaskMetaData.getTaskId());

        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        try {
            // 将 JobDetail 和 Trigger 关联起来并调度任务
            // 将任务调度到 Quartz 调度器中。它并不直接执行任务，而是安排任务在指定的时间触发
            scheduler.scheduleJob(jobDetail, trigger);
            // 重置任务状态为已添加
            // 在事件监听器中，当任务事件触发时，你需要确保任务的状态被正确重置，以便 Quartz 调度器可以重新调度和执行该任务
            scheduledTaskRegistry.resetCanceled(scheduledTaskMetaData.getTaskId());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
