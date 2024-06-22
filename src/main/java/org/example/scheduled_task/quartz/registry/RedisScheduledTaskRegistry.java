package org.example.scheduled_task.quartz.registry;

import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
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

    private HashOperations<String, String, Object> hashOps;

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
}
