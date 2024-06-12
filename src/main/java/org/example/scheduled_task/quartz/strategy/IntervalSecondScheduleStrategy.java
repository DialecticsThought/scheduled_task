package org.example.scheduled_task.quartz.strategy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/05/23 15:34
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class IntervalSecondScheduleStrategy implements ScheduleStrategy {
    private Integer interval;

    //private TimeUnit unit;

    @Override
    public Trigger getTrigger(String taskName) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(taskName + "_trigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .build();
    }
}
