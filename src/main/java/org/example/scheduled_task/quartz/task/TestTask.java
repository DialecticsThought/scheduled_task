package org.example.scheduled_task.quartz.task;

import jakarta.annotation.Resource;
import org.example.scheduled_task.quartz.entity.SpringContextHolder;

import java.time.LocalDateTime;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/11 19:12
 */
public class TestTask implements ExecutedTask<Void> {

    @Resource
    private SpringContextHolder springContextHolder;

    @Override
    public Void execute() {
        System.out.println("hello,world" + LocalDateTime.now());
        return null;
    }
}
