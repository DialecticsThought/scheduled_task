package org.example.scheduled_task.controller;

import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
import org.example.scheduled_task.quartz.strategy.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.TestTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/11 19:09
 */
@RestController
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private TaskEventPublisher taskEventPublisher;

    @GetMapping("/schedule")
    public String scheduleTask(@RequestParam("cronExpression") String cronExpression) {
        CronScheduleStrategy cronScheduleStrategy = new CronScheduleStrategy(cronExpression);

        TestTask testTask = new TestTask();

        taskEventPublisher.publishTaskEvent(new ScheduledTaskMetaData<>(cronScheduleStrategy, testTask), "test");
        return "success";
    }
}
