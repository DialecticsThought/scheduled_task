package org.example.scheduled_task.quartz.strategy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/05/23 15:32
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CronScheduleStrategy implements ScheduleStrategy {

    private String cronExpression;

    @Override
    public Trigger getTrigger(String taskName) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(taskName + "_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}
