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
        String taskClassPath = coarseScheduledTaskMetaData.getTaskClassPath();
        String cronExpression = coarseScheduledTaskMetaData.getCronExpression();
        String taskName = coarseScheduledTaskMetaData.getTaskName();
        Map<String, Object> properties = coarseScheduledTaskMetaData.getProperties();

        // 检查是否已有相同的taskId对象
        if (((ConfigurableApplicationContext) applicationContext).getBeanFactory().containsSingleton(taskId)) {
            throw new IllegalArgumentException("任务ID " + taskId + " 已经存在，请使用不同的任务ID。");
        }
        ExecutedTask instance = null;
        try {
            String beanName = "quartz_task:" + taskId;
            // 获取 Class 对象
            Class<?> clazz = Class.forName(taskClassPath);
            // 检查是否实现了指定接口
            if (ExecutedTask.class.isAssignableFrom(clazz)) {
                instance = (ExecutedTask) beanManager.createAndRegisterBean(beanName, taskClassPath);
            } else {
                throw new IllegalArgumentException(taskClassPath + " 未实现接口 " + ExecutedTask.class.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("创建任务实例失败", e);
        }
        // TODO 如果对象有依赖注入，交给容器管理之后，会自动注入

        // 将 instance 的引用赋值给一个局部变量 finalInstance，
        // 可以避免在 lambda 表达式中直接使用 instance，并且无需将 instance 声明为 final。
        ExecutedTask finalInstance = instance;
        // 使用传入的properties参数给ExecutedTask实例赋值
        if (properties != null) {
            properties.forEach((key, value) -> {
                try {
                    // 获取对应的字段
                    Field field = getField(finalInstance.getClass(), key);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(finalInstance, value);
                    } else {
                        throw new NoSuchFieldException("Field " + key + " not found in class " + finalInstance.getClass().getName());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("设置属性 " + key + " 失败", e);
                }
            });
        }
        CronScheduleStrategy cronScheduleStrategy = new CronScheduleStrategy(cronExpression);

        ScheduledTaskMetaData<?> scheduledTaskMetaData =
                new ScheduledTaskMetaData<>(taskId, taskName, cronScheduleStrategy, instance, properties);

        addTask(scheduledTaskMetaData);
    }

    // 辅助方法，用于在类层次结构中查找字段
    private Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
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
