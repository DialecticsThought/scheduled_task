package org.example.scheduled_task.quartz.registry;

import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author jiahao.liu
 * @Data 2024/6/21 23:48
 */
@Component
public class RedisScheduledTaskRegistry implements ScheduledTaskRegistry {

    private static final String TASK_META_DATA_KEY_PREFIX = "taskMetaData:";

    private static final String TASK_STATUS_KEY_PREFIX = "taskStatus:";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;

    private ValueOperations<String, Object> valueOps;

    @Autowired
    public void setValueOps(RedisTemplate<String, Object> redisTemplate) {
        this.valueOps = redisTemplate.opsForValue();
    }

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return (ScheduledTaskMetaData<?>) valueOps.get(TASK_META_DATA_KEY_PREFIX + taskId);
    }

    @Override
    public void registerTask(String taskId, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        valueOps.set(TASK_META_DATA_KEY_PREFIX + taskId, scheduledTaskMetaData);
        valueOps.set(TASK_STATUS_KEY_PREFIX + taskId, TaskStatus.ADDED);
    }

    @Override
    public void markExecute(String taskId) {
        if (containsTask(taskId) && getTaskStatus(taskId) != TaskStatus.EXECUTED) {
            valueOps.set(TASK_STATUS_KEY_PREFIX + taskId, TaskStatus.EXECUTED);
        }
    }

    @Override
    public void removeTask(String taskId) {
        redisTemplate.delete(TASK_META_DATA_KEY_PREFIX + taskId);
    }

    @Override
    public boolean containsTask(String taskId) {
        return redisTemplate.hasKey(TASK_META_DATA_KEY_PREFIX + taskId);
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
            valueOps.set(TASK_STATUS_KEY_PREFIX + taskId, TaskStatus.CANCELED);
        }
    }

    @Override
    public void deleteTask(String taskId) {
        if (containsTask(taskId)) {
            cancelTask(taskId);
            removeTask(taskId);
            valueOps.set(TASK_STATUS_KEY_PREFIX + taskId, TaskStatus.DELETED);
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
            valueOps.set(TASK_STATUS_KEY_PREFIX + taskId, TaskStatus.ADDED);
        }
    }

    private TaskStatus getTaskStatus(String taskId) {
        return (TaskStatus) valueOps.get(TASK_STATUS_KEY_PREFIX + taskId);
    }
}
