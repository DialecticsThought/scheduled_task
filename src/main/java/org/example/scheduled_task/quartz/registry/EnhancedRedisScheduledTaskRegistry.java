package org.example.scheduled_task.quartz.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.util.TaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * 注册任务：
 *
 * 使用 getProperties 方法递归获取任务对象的所有自定义属性和依赖注入的属性。
 * 将所有属性序列化为 JSON 字符串，并存储在 TaskProperties 表中。
 * 将任务元数据存储到 Redis 哈希表中。
 * 恢复任务：
 *
 * 从 Redis 哈希表中读取任务对象的类路径，并动态加载该类。
 * 实例化任务对象，并使用 Spring 容器注入其依赖。
 * 使用 setProperties 方法递归设置任务对象的所有自定义属性和依赖注入的属性。
 * 如果属性本身是对象，则递归设置其属性，并根据需要注入依赖
 * @Author veritas
 * @Data 2024/6/22 16:44
 */
@Component
public class EnhancedRedisScheduledTaskRegistry implements ScheduledTaskRegistry {

    private final RedisScheduledTaskRegistry redisScheduledTaskRegistry;

    @Autowired
    public EnhancedRedisScheduledTaskRegistry(RedisScheduledTaskRegistry redisScheduledTaskRegistry) {
        this.redisScheduledTaskRegistry = redisScheduledTaskRegistry;
    }

    @Resource
    private AutowireCapableBeanFactory beanFactory;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return redisScheduledTaskRegistry.getScheduledTaskMetaData(taskId);
    }

    @Override
    public void registerTask(String taskId, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        try {
            if (scheduledTaskMetaData.getExecutedTask() != null) {
                // 获取任务自定义属性和依赖注入的属性
                Map<String, Object> properties = TaskUtils.getProperties(scheduledTaskMetaData.getExecutedTask());
                scheduledTaskMetaData.setProperties(properties);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        redisScheduledTaskRegistry.registerTask(taskId, scheduledTaskMetaData);
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

}
