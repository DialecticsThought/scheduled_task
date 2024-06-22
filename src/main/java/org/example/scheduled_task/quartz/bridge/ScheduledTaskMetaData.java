package org.example.scheduled_task.quartz.bridge;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.scheduled_task.quartz.strategy.ScheduleStrategy;
import org.example.scheduled_task.quartz.task.ExecutedTask;

import java.util.Map;

/**
 * @author jiahao.liu
 * @description 把 调度策略 和 任务执行 桥接起来
 * @date 2024/05/23 15:37
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ScheduledTaskMetaData<T> {
    /**
     * 任务id
     */
    private String taskId;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 任务调度策略
     */
    private ScheduleStrategy scheduleStrategy;
    /**
     * 任务
     */
    private ExecutedTask<T> executedTask;
    /**
     * 任务属性
     */
    private Map<String, Object> properties;


    public T execute() {
        return executedTask.execute();
    }
}
