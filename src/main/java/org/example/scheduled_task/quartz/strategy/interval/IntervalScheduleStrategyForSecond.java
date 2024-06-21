package org.example.scheduled_task.quartz.strategy.interval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.scheduled_task.quartz.strategy.ScheduleStrategy;
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
public class IntervalScheduleStrategyForSecond implements ScheduleStrategy {
    /**
     * 秒数
     */
    private Integer second;

    @Override
    public Trigger getTrigger(String taskName) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(taskName + "_trigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(second).repeatForever())
                .build();
    }
}
