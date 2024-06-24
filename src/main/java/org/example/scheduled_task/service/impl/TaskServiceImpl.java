package org.example.scheduled_task.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.entity.CoarseScheduledTaskMetaData;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.entity.BeanManager;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
import org.example.scheduled_task.quartz.registry.DatabaseTaskRegistry;
import org.example.scheduled_task.quartz.strategy.cron.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.ExecutedTask;
import org.example.scheduled_task.service.TaskService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/12 13:43
 */
@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private BeanManager beanManager;
    @Resource
    private TaskEventPublisher taskEventPublisher;
    @Resource
    private DatabaseTaskRegistry scheduledTaskRegistry;

    @Override
    @Transactional
    public void addTaskCompletely(CoarseScheduledTaskMetaData coarseScheduledTaskMetaData) {
        String taskId = coarseScheduledTaskMetaData.getTaskId();
        String cronExpression = coarseScheduledTaskMetaData.getCronExpression();
        String taskName = coarseScheduledTaskMetaData.getTaskName();
        Map<String, Object> properties = coarseScheduledTaskMetaData.getProperties();

        // 检查是否已有相同的taskId对象
        if (((ConfigurableApplicationContext) applicationContext).getBeanFactory().containsSingleton(taskId)) {
            throw new IllegalArgumentException("任务ID " + taskId + " 已经存在，请使用不同的任务ID。");
        }
        ExecutedTask instance = beanManager.createTaskInstance(coarseScheduledTaskMetaData);
        // TODO 如果对象有依赖注入，交给容器管理之后，会自动注入

        beanManager.assignValue(instance, properties);

        CronScheduleStrategy cronScheduleStrategy = new CronScheduleStrategy(cronExpression);

        ScheduledTaskMetaData<?> scheduledTaskMetaData =
                new ScheduledTaskMetaData<>(taskId, taskName, cronScheduleStrategy, instance, properties);

        addTask(scheduledTaskMetaData);
    }


    @Override
    @Transactional
    public void addTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        if (!scheduledTaskRegistry.containsTask(scheduledTaskMetaData.getTaskId())) {
            scheduledTaskRegistry.registerTask(scheduledTaskMetaData);
        }
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        if (scheduledTaskRegistry.containsTask(taskId)) {
            scheduledTaskRegistry.deleteTask(taskId);
        }
    }

    /**
     * TODO 重点 在这里执行任务，本质是发布事件，监听器收到事件，把事件对象中的任务信息注册
     *
     * @param taskId
     */
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
                // 发布事件
                taskEventPublisher.publishTaskEvent(taskMetaData);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void cancelTask(String taskId) {
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.cancelTask(taskId);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
