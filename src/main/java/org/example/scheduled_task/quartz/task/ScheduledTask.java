package org.example.scheduled_task.quartz.task;

/**
 * @author jiahao.liu
 * @description 定时任务的通用任务接口
 * 泛型是因为 可以执行返回类型 甚至是不返回
 * @date 2024/05/23 15:29
 */
@FunctionalInterface
public interface ScheduledTask<T> {

    T execute();
}
