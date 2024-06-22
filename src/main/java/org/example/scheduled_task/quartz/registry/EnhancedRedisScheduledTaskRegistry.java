package org.example.scheduled_task.quartz.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

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
public class EnhancedRedisScheduledTaskRegistry implements ScheduledTaskRegistry {

    private static final String TASK_META_DATA_KEY_PREFIX = "taskMetaData:";

    private static final String TASK_STATUS_KEY_PREFIX = "taskStatus:";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;

    private HashOperations<String, String, Object> hashOps;

    @Resource
    private AutowireCapableBeanFactory beanFactory;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public void setHashOps(RedisTemplate<String, Object> redisTemplate) {
        this.hashOps = redisTemplate.opsForHash();
    }

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return (ScheduledTaskMetaData<?>) hashOps.get(TASK_META_DATA_KEY_PREFIX, taskId);
    }

    @Override
    public void registerTask(String taskId, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        try {
            if (scheduledTaskMetaData.getExecutedTask() != null) {
                // 获取任务自定义属性和依赖注入的属性
                Map<String, Object> properties = getProperties(scheduledTaskMetaData.getExecutedTask());
                scheduledTaskMetaData.setProperties(properties);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        hashOps.put(TASK_META_DATA_KEY_PREFIX, taskId, scheduledTaskMetaData);
        hashOps.put(TASK_STATUS_KEY_PREFIX, taskId, TaskStatus.ADDED.toValue());
    }

    @Override
    public void markExecute(String taskId) {
        if (containsTask(taskId) && getTaskStatus(taskId) != TaskStatus.EXECUTED) {
            hashOps.put(TASK_STATUS_KEY_PREFIX, taskId, TaskStatus.EXECUTED.toValue());
        }
    }

    @Override
    public void removeTask(String taskId) {
        hashOps.delete(TASK_META_DATA_KEY_PREFIX, taskId);
    }

    @Override
    public boolean containsTask(String taskId) {
        return hashOps.hasKey(TASK_META_DATA_KEY_PREFIX, taskId);
    }

    @Override
    public void cancelTask(String taskId) {
        if (containsTask(taskId)) {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            try {
                scheduler.deleteJob(new JobKey(taskId));
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
            hashOps.put(TASK_STATUS_KEY_PREFIX, taskId, TaskStatus.CANCELED.toValue());
        }
    }

    @Override
    public void deleteTask(String taskId) {
        if (containsTask(taskId)) {
            cancelTask(taskId);
            removeTask(taskId);
            hashOps.put(TASK_STATUS_KEY_PREFIX, taskId, TaskStatus.DELETED.toValue());
        }
    }

    @Override
    public boolean isCanceled(String taskId) {
        return TaskStatus.CANCELED.equals(getTaskStatus(taskId));
    }

    @Override
    public boolean isDeleted(String taskId) {
        return !containsTask(taskId);
    }

    @Override
    public void resetCanceled(String taskId) {
        if (TaskStatus.CANCELED.equals(getTaskStatus(taskId))) {
            hashOps.put(TASK_STATUS_KEY_PREFIX, taskId, TaskStatus.ADDED.toValue());
        }
    }

    private TaskStatus getTaskStatus(String taskId) {
        String status = (String) hashOps.get(TASK_STATUS_KEY_PREFIX, taskId);
        return TaskStatus.valueOf(status);
    }

    private Map<String, Object> getProperties(Object task) throws IllegalAccessException {
        // 使用反射递归获取任务实例的自定义属性和依赖注入的属性
        Map<String, Object> properties = new HashMap<>();
        for (Field field : task.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(task);
            if (value != null && !isPrimitiveOrWrapper(value.getClass()) && !value.getClass().equals(String.class)) {
                // 递归处理对象属性
                properties.put(field.getName(), getProperties(value));
            } else {
                properties.put(field.getName(), value);
            }
        }
        return properties;
    }

    private void setProperties(Object task, Map<String, Object> properties) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        // 使用反射递归设置任务实例的自定义属性和依赖注入的属性
        for (Field field : task.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = properties.get(field.getName());
            if (value != null && !isPrimitiveOrWrapper(field.getType()) && !field.getType().equals(String.class)) {
                // 如果属性是对象，递归设置属性
                Object nestedObject = field.getType().getDeclaredConstructor().newInstance();
                setProperties(nestedObject, (Map<String, Object>) value);
                field.set(task, nestedObject);
                // 判断是否为依赖注入
                if (field.isAnnotationPresent(Autowired.class)) {
                    beanFactory.autowireBean(nestedObject);
                }
            } else {
                field.set(task, value);
            }
        }
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Void.class);
    }
}
