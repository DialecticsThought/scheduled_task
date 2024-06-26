package org.example.scheduled_task.quartz.task;

/**
 * @author jiahao.liu
 * @description 定时任务的通用任务接口
 * 泛型是因为 可以执行返回类型 甚至是不返回
 * 所有任务类都要加上 注解 @Scope("prototype")  ☆☆☆☆☆☆☆☆☆☆
 * @date 2024/05/23 15:29
 */
@FunctionalInterface
public interface ExecutedTask<T> {

    T execute();
}
