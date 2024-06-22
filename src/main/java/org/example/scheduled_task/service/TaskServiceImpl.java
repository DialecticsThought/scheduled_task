package org.example.scheduled_task.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
import org.example.scheduled_task.quartz.registry.EnhancedRedisScheduledTaskRegistry;
import org.example.scheduled_task.quartz.strategy.cron.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.ExecutedTask;
import org.example.scheduled_task.quartz.registry.ScheduledTaskRegistry;
import org.example.scheduled_task.quartz.util.TaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

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
    private AutowireCapableBeanFactory beanFactory;
    @Resource
    private TaskEventPublisher taskEventPublisher;
    @Resource
    private EnhancedRedisScheduledTaskRegistry scheduledTaskRegistry;

    @Override
    public void addTaskCompletely(String cronExpression, String taskId,
                                  String taskName, String taskClassPath) {
        // 检查是否已有相同的taskId对象
        if (((ConfigurableApplicationContext) applicationContext).getBeanFactory().containsSingleton(taskId)) {
            throw new IllegalArgumentException("任务ID " + taskId + " 已经存在，请使用不同的任务ID。");
        }
        ExecutedTask instance = null;
        try {
            // 获取 Class 对象
            Class<?> clazz = Class.forName(taskClassPath);
            // 检查是否实现了指定接口
            if (ExecutedTask.class.isAssignableFrom(clazz)) {
                // 创建对象实例
                instance = (ExecutedTask<?>) clazz.getDeclaredConstructor().newInstance();
                // 使用 autowireBean 方法将对象的依赖注入到对象中
                applicationContext.getAutowireCapableBeanFactory().autowireBean(instance);
                // 注册Bean到Spring容器
                ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
                configurableApplicationContext.getBeanFactory().registerSingleton(taskId, instance);

                System.out.println("对象创建并注入容器成功：" + instance);
            } else {
                throw new IllegalArgumentException(taskClassPath + " 未实现接口 " + ExecutedTask.class.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("创建任务实例失败", e);
        }

        // 获取任务实例的属性
        Map<String, Object> properties = null;
        try {
            properties = TaskUtils.getProperties(instance);

            System.out.println(properties);
        } catch (Exception e) {
            throw new RuntimeException("获取任务属性失败", e);
        }
        CronScheduleStrategy cronScheduleStrategy = new CronScheduleStrategy(cronExpression);

        ScheduledTaskMetaData<?> scheduledTaskMetaData =
                new ScheduledTaskMetaData<>(taskId, taskName, cronScheduleStrategy, instance, null);

        addTask(scheduledTaskMetaData);
    }

    @Override
    public void addTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        if (!scheduledTaskRegistry.containsTask(scheduledTaskMetaData.getTaskId())) {
            scheduledTaskRegistry.registerTask(scheduledTaskMetaData.getTaskId(), scheduledTaskMetaData);
        }
    }

    @Override
    public void deleteTask(String taskId) {
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.deleteTask(taskId);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

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
