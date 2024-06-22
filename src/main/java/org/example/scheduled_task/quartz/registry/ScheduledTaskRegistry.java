package org.example.scheduled_task.quartz.registry;

import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/21 23:45
 */
public interface ScheduledTaskRegistry {
    /**
     * 得到任务元信息
     *
     * @param taskId
     * @return
     */
    ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId);

    /**
     * 注册任务
     *
     * @param taskId
     * @param scheduledTaskMetaData
     */
    void registerTask(String taskId, ScheduledTaskMetaData<?> scheduledTaskMetaData);

    /**
     * 标记任务已经执行
     *
     * @param taskId
     */
    void markExecute(String taskId);

    /**
     * 删除任务
     *
     * @param taskId
     */
    void removeTask(String taskId);

    /**
     * 判断是否存在该任务
     *
     * @param taskId
     * @return
     */
    boolean containsTask(String taskId);

    /**
     * 取消任务
     *
     * @param taskId
     * @throws Exception
     */
    void cancelTask(String taskId);

    /**
     * 删除任务
     *
     * @param taskId
     * @throws Exception
     */
    void deleteTask(String taskId);

    /**
     * 判断任务是否取消
     *
     * @param taskId
     * @return
     */
    boolean isCanceled(String taskId);

    /**
     * 判断任务是否被删除
     *
     * @param taskId
     * @return
     */
    boolean isDeleted(String taskId);

    /**
     * 重制取消状态 -> 添加状态
     *
     * @param taskId
     */
    void resetCanceled(String taskId);
}
