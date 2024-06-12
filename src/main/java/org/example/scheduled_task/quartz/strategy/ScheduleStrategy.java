package org.example.scheduled_task.quartz.strategy;

import org.quartz.Trigger;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/05/23 15:31
 */
public interface ScheduleStrategy {

    Trigger getTrigger(String taskName);
}
