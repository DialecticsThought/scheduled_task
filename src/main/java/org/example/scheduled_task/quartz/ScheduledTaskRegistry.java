package org.example.scheduled_task.quartz;


import cn.hutool.core.collection.ConcurrentHashSet;
import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiahao.liu
 * @description 注册中心
 * @date 2024/05/23 15:42
 */
@Component
public class ScheduledTaskRegistry {
    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;

    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();

    private final Map<String, ScheduledTaskMetaData<?>> taskMap = new ConcurrentHashMap<>();

    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return taskMap.get(taskId);
    }

    public void registerTask(String taskId, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        taskMap.put(taskId, scheduledTaskMetaData);
        taskStatusMap.put(taskId, TaskStatus.ADD);
    }

    public void markExecute(String taskId) {
        if (taskStatusMap.containsKey(taskId) && taskStatusMap.get(taskId) != TaskStatus.EXECUTED) {
            // 更新任务状态
            taskStatusMap.put(taskId, TaskStatus.EXECUTED);
        }
    }

    public void removeTask(String taskId) {
        taskMap.remove(taskId);
    }

    public boolean containsTask(String taskId) {
        return taskMap.containsKey(taskId);
    }

    public void cancelTask(String taskId) throws Exception {
        // 是否存在任务
        if (containsTask(taskId)) {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            scheduler.deleteJob(new JobKey(taskId));
            // 更新任务状态
            taskStatusMap.put(taskId, TaskStatus.CANCEL);
        }
    }

    public void deleteTask(String taskId) throws Exception {
        // 是否存在任务
        if (containsTask(taskId)) {
            // 取消任务
            cancelTask(taskId);
            // 删除任务
            removeTask(taskId);
            // 更新任务状态
            taskStatusMap.put(taskId, TaskStatus.DELETE);
        }
    }

    public boolean isCanceled(String taskId) {
        return taskStatusMap.getOrDefault(taskId, TaskStatus.NONE).equals(TaskStatus.CANCEL);
    }

    public boolean isDeleted(String taskId) {
        return taskStatusMap.getOrDefault(taskId, TaskStatus.NONE).equals(TaskStatus.DELETE);
    }

    /**
     * 重制取消状态 -> 添加状态
     *
     * @param taskId
     */
    public void resetCanceled(String taskId) {
        if (taskStatusMap.containsKey(taskId) && taskStatusMap.get(taskId).equals(TaskStatus.CANCEL)) {
            taskStatusMap.put(taskId, TaskStatus.ADD);
        }
    }
}