package org.example.scheduled_task.quartz.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.entity.BeanManager;
import org.example.scheduled_task.quartz.strategy.cron.CronScheduleStrategy;
import org.example.scheduled_task.service.TaskService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Description 注册任务：
 * <p>
 * 使用 getProperties 方法递归获取任务对象的所有自定义属性和依赖注入的属性。
 * 将所有属性序列化为 JSON 字符串，并存储在 TaskProperties 表中。
 * 将任务元数据存储到 Redis 哈希表中。
 * 恢复任务：
 * <p>
 * 从 Redis 哈希表中读取任务对象的类路径，并动态加载该类。
 * 实例化任务对象，并使用 Spring 容器注入其依赖。
 * 使用 setProperties 方法递归设置任务对象的所有自定义属性和依赖注入的属性。
 * 如果属性本身是对象，则递归设置其属性，并根据需要注入依赖
 * @Author veritas
 * @Data 2024/6/22 16:44
 */
@Component
@Slf4j
public class EnhancedRedisScheduledTaskRegistry implements ScheduledTaskRegistry {

    private final RedisScheduledTaskRegistry redisScheduledTaskRegistry;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private BeanManager beanManager;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public EnhancedRedisScheduledTaskRegistry(RedisScheduledTaskRegistry redisScheduledTaskRegistry) {
        this.redisScheduledTaskRegistry = redisScheduledTaskRegistry;
    }

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return redisScheduledTaskRegistry.getScheduledTaskMetaData(taskId);
    }

    @Override
    public void registerTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        try {
            if (scheduledTaskMetaData.getExecutedTask() != null) {
                // 获取任务自定义属性和依赖注入的属性
                Map<String, Object> properties = BeanManager.getProperties(scheduledTaskMetaData.getExecutedTask());
                scheduledTaskMetaData.setProperties(properties);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        redisScheduledTaskRegistry.registerTask(scheduledTaskMetaData);
    }


    @Override
    public void markExecute(String taskId) {
        redisScheduledTaskRegistry.markExecute(taskId);
    }

    @Override
    public void removeTask(String taskId) {
        redisScheduledTaskRegistry.removeTask(taskId);
    }

    @Override
    public boolean containsTask(String taskId) {
        return redisScheduledTaskRegistry.containsTask(taskId);
    }

    @Override
    public void cancelTask(String taskId) {
        redisScheduledTaskRegistry.cancelTask(taskId);
    }

    @Override
    public void deleteTask(String taskId) {
        redisScheduledTaskRegistry.deleteTask(taskId);
        removeTaskFromSpringContainer(taskId);
    }

    @Override
    public boolean isCanceled(String taskId) {
        return redisScheduledTaskRegistry.isCanceled(taskId);
    }

    @Override
    public boolean isDeleted(String taskId) {
        return redisScheduledTaskRegistry.isDeleted(taskId);
    }

    @Override
    public void resetCanceled(String taskId) {
        redisScheduledTaskRegistry.resetCanceled(taskId);
    }

    private void removeTaskFromSpringContainer(String taskId) {
        String beanName = taskId + ":quartz_task";
        beanManager.removeBeanByName(beanName);
    }

    /**
     * 待定
     */
    public void initializeTasksFromRedis() {
        // 从Redis中读取所有任务ID
        Map<String, ScheduledTaskMetaData<?>> allTasks = redisScheduledTaskRegistry.getAllTasks();
        for (Map.Entry<String, ScheduledTaskMetaData<?>> entry : allTasks.entrySet()) {
            String taskId = entry.getKey();
            ScheduledTaskMetaData<?> metaData = entry.getValue();
            // 从Redis中读取任务状态
            TaskStatus taskStatus = redisScheduledTaskRegistry.getTaskStatus(taskId);
            // 重新创建任务对象并注册
            try {
                // 检查是否已有相同的taskId对象
                if (((ConfigurableApplicationContext) applicationContext).getBeanFactory().containsSingleton(taskId)) {
                    log.info("任务ID: {} 已经存在，请使用不同的任务ID。", taskId);

                }
            } catch (Exception e) {
                log.error("重新创建任务失败，taskId：{}", taskId);
            }
        }
    }
}
