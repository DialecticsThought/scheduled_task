package org.example.scheduled_task.quartz.registry;


import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiahao.liu
 * @description 注册中心
 * @date 2024/05/23 15:42
 */
//@Component
public class MemoryScheduledTaskRegistry implements ScheduledTaskRegistry{
    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;
    /**
     * 存放任务状态的 key=任务标识符 value=任务当前状态
     */
    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    /**
     * 存放任务元数据 key=任务标识符 value=任务所有信息
     */
    private final Map<String, ScheduledTaskMetaData<?>> taskMap = new ConcurrentHashMap<>();

    /**
     * 得到任务元信息
     * @param taskId
     * @return
     */
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return taskMap.get(taskId);
    }

    /**
     * 注册任务
     *
     * @param scheduledTaskMetaData
     */
    public void registerTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        taskMap.put(scheduledTaskMetaData.getTaskId(), scheduledTaskMetaData);
        taskStatusMap.put(scheduledTaskMetaData.getTaskId(), TaskStatus.ADDED);
    }

    /**
     * 标记任务已经执行
     *
     * @param taskId
     */
    public void markExecute(String taskId) {
        if (taskStatusMap.containsKey(taskId) && taskStatusMap.get(taskId) != TaskStatus.EXECUTED) {
            // 更新任务状态
            taskStatusMap.put(taskId, TaskStatus.EXECUTED);
        }
    }

    /**
     * 删除任务
     *
     * @param taskId
     */
    public void removeTask(String taskId) {
        taskMap.remove(taskId);
    }

    /**
     * 判断是否存在该任务
     *
     * @param taskId
     * @return
     */
    public boolean containsTask(String taskId) {
        return taskMap.containsKey(taskId);
    }


    /**
     * 取消任务
     *
     * @param taskId
     * @throws Exception
     */
    public void cancelTask(String taskId)  {
        // 是否存在任务
        if (containsTask(taskId)) {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            try {
                scheduler.deleteJob(new JobKey(taskId));
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
            // 更新任务状态
            taskStatusMap.put(taskId, TaskStatus.CANCELED);
        }
    }

    /**
     * 删除任务
     * @param taskId
     * @throws Exception
     */
    public void deleteTask(String taskId) {
        // 是否存在任务
        if (containsTask(taskId)) {
            // 取消任务
            cancelTask(taskId);
            // 删除任务
            removeTask(taskId);
            // 更新任务状态
            taskStatusMap.put(taskId, TaskStatus.DELETED);
        }
    }

    /**
     * 判断任务是否取消
     *
     * @param taskId
     * @return
     */
    public boolean isCanceled(String taskId) {
        return taskStatusMap.getOrDefault(taskId, TaskStatus.NONE).equals(TaskStatus.CANCELED);
    }

    /**
     * 判断任务是否被删除
     *
     * @param taskId
     * @return
     */
    public boolean isDeleted(String taskId) {
        //return taskStatusMap.getOrDefault(taskId, TaskStatus.NONE).equals(TaskStatus.DELETED);
        return taskMap.containsKey(taskId);
    }

    /**
     * 重制取消状态 -> 添加状态
     *
     * @param taskId
     */
    public void resetCanceled(String taskId) {
        if (taskStatusMap.containsKey(taskId) && taskStatusMap.get(taskId).equals(TaskStatus.CANCELED)) {
            taskStatusMap.put(taskId, TaskStatus.ADDED);
        }
    }
}
