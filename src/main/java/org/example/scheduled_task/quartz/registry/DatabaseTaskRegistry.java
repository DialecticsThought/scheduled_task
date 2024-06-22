package org.example.scheduled_task.quartz.registry;

import jakarta.annotation.Resource;
import org.example.scheduled_task.entity.ScheduledTask;
import org.example.scheduled_task.mapper.ScheduledTaskMapper;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.strategy.cron.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.ExecutedTask;


/**
 * @Description
 * @Author veritas
 * @Data 2024/6/22 15:17
 */
public class DatabaseTaskRegistry implements ScheduledTaskRegistry {

    @Resource
    private ScheduledTaskMapper taskMapper;

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        ScheduledTask scheduledTask = taskMapper.selectById(taskId);
        if (scheduledTask != null) {
            return convertToMetaData(scheduledTask);
        }
        return null;
    }

    @Override
    public void registerTask(String taskId, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        ScheduledTask scheduledTask = convertToEntity(scheduledTaskMetaData);
        scheduledTask.setTaskStatus(TaskStatus.ADDED.toValue());
        taskMapper.insert(scheduledTask);
    }

    @Override
    public void markExecute(String taskId) {
        ScheduledTask scheduledTask = taskMapper.selectById(taskId);
        if (scheduledTask != null && !TaskStatus.EXECUTED.toValue().equals(scheduledTask.getTaskStatus())) {
            scheduledTask.setTaskStatus(TaskStatus.EXECUTED.toValue());
            taskMapper.updateById(scheduledTask);
        }
    }

    @Override
    public void removeTask(String taskId) {
        taskMapper.deleteById(taskId);
    }

    @Override
    public boolean containsTask(String taskId) {
        return taskMapper.selectById(taskId) != null;
    }

    @Override
    public void cancelTask(String taskId) {
        ScheduledTask scheduledTask = taskMapper.selectById(taskId);
        if (scheduledTask != null) {
            scheduledTask.setTaskStatus(TaskStatus.CANCELED.toValue());
            taskMapper.updateById(scheduledTask);
        }
    }

    @Override
    public void deleteTask(String taskId) {
        ScheduledTask scheduledTask = taskMapper.selectById(taskId);
        if (scheduledTask != null) {
            scheduledTask.setTaskStatus(TaskStatus.DELETED.toValue());
            taskMapper.updateById(scheduledTask);
            taskMapper.deleteById(taskId);
        }
    }

    @Override
    public boolean isCanceled(String taskId) {
        ScheduledTask scheduledTask = taskMapper.selectById(taskId);
        return scheduledTask != null && TaskStatus.CANCELED.toValue().equals(scheduledTask.getTaskStatus());
    }

    @Override
    public boolean isDeleted(String taskId) {
        return taskMapper.selectById(taskId) == null;
    }

    @Override
    public void resetCanceled(String taskId) {
        ScheduledTask scheduledTask = taskMapper.selectById(taskId);
        if (scheduledTask != null && TaskStatus.CANCELED.toValue().equals(scheduledTask.getTaskStatus())) {
            scheduledTask.setTaskStatus(TaskStatus.ADDED.toValue());
            taskMapper.updateById(scheduledTask);
        }
    }

    private ScheduledTask convertToEntity(ScheduledTaskMetaData<?> metaData) {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setTaskId(metaData.getTaskId());
        scheduledTask.setTaskName(metaData.getTaskName());
        scheduledTask.setCronExpression(((CronScheduleStrategy) metaData.getScheduleStrategy()).getCronExpression());
        scheduledTask.setTaskClassPath(metaData.getExecutedTask().getClass().getName());
        return scheduledTask;
    }

    private ScheduledTaskMetaData<?> convertToMetaData(ScheduledTask scheduledTask) {
        ScheduledTaskMetaData<?> metaData = new ScheduledTaskMetaData<>();
        metaData.setTaskId(scheduledTask.getTaskId());
        metaData.setTaskName(scheduledTask.getTaskName());
        metaData.setScheduleStrategy(new CronScheduleStrategy(scheduledTask.getCronExpression()));
        try {
            Class<?> taskClass = Class.forName(scheduledTask.getTaskClassPath());
            ExecutedTask taskInstance = (ExecutedTask) taskClass.getDeclaredConstructor().newInstance();
            metaData.setExecutedTask(taskInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return metaData;
    }
}
