package org.example.scheduled_task.quartz.factory;


import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.ScheduledTaskRegistry;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.task.ScheduledTask;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * @author jiahao.liu
 * @description 可以认为 实现方法execute的作用是： 从注册表中获取任务并执行
 * @date 2024/05/23 16:09
 */
@Component
public class QuartzJobFactory implements Job {
    @Resource
    private ScheduledTaskRegistry scheduledTaskRegistry;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String taskName = context.getJobDetail().getKey().getName();

        ScheduledTaskMetaData<?> scheduledTaskMetaData = scheduledTaskRegistry.getTaskWithStrategy(taskName);

        ScheduledTask<?> scheduledTask = scheduledTaskMetaData.getScheduledTask();

        if (scheduledTask != null) {
            scheduledTask.execute();
        }
    }
}
