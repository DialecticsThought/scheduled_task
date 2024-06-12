package org.example.scheduled_task.quartz.bridge;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.scheduled_task.quartz.strategy.ScheduleStrategy;
import org.example.scheduled_task.quartz.task.ScheduledTask;

/**
 * @author jiahao.liu
 * @description 把 调度策略 和 任务执行 桥接起来
 * @date 2024/05/23 15:37
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ScheduledTaskMetaData<T> {
    private String taskId;

    private String taskName;

    private ScheduleStrategy scheduleStrategy;

    private ScheduledTask<T> scheduledTask;

    public T execute() {
        return scheduledTask.execute();
    }
}
