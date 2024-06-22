package org.example.scheduled_task.quartz.task;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/22 18:00
 */
@Component
public class Test {

    public void test() {
        System.out.println("hello,world" + LocalDateTime.now());
    }
}
