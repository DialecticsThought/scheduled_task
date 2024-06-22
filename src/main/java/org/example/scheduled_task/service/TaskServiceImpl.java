package org.example.scheduled_task.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.event.TaskEventPublisher;
import org.example.scheduled_task.quartz.strategy.cron.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.ExecutedTask;
import org.example.scheduled_task.quartz.registry.ScheduledTaskRegistry;
import org.springframework.stereotype.Service;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/12 13:43
 */
@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Resource
    private TaskEventPublisher taskEventPublisher;
    @Resource
    private ScheduledTaskRegistry scheduledTaskRegistry;

    @Override
    public void addTaskCompletely(String cronExpression, String taskId,
                                  String taskName, String taskClassPath) {
        ExecutedTask instance = null;
        try {
            // 接口类
            Class<?> interfaceClass = ExecutedTask.class;
            // 获取 Class 对象
            Class<?> clazz = Class.forName(taskClassPath);
            // 检查是否实现了指定接口
            if (interfaceClass.isAssignableFrom(clazz)) {
                // 创建对象实例
                instance = (ExecutedTask) clazz.getDeclaredConstructor().newInstance();
                System.out.println("对象创建成功：" + instance);
            } else {
                throw new IllegalArgumentException(taskClassPath + " 未实现接口 " + interfaceClass.getName());
            }
        } catch (Exception e) {

        }
        CronScheduleStrategy cronScheduleStrategy = new CronScheduleStrategy(cronExpression);

        ScheduledTaskMetaData<Void> scheduledTaskMetaData =
                new ScheduledTaskMetaData<>(taskId, taskName, cronScheduleStrategy, instance);

        addTask(scheduledTaskMetaData);
    }


    @Override
    public void addTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        if (!scheduledTaskRegistry.containsTask(scheduledTaskMetaData.getTaskId())) {

            scheduledTaskRegistry.registerTask(scheduledTaskMetaData.getTaskId(), scheduledTaskMetaData);
        }
        log.info("");
    }

    @Override
    public void deleteTask(String taskId) {
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.deleteTask(taskId);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void executeTask(String taskId) {
        try {
            ScheduledTaskMetaData<?> taskMetaData = scheduledTaskRegistry.getScheduledTaskMetaData(taskId);
            if (taskMetaData != null) {
                // 当调用 executeTask 方法时，如果任务之前被取消，那么你需要重置任务的状态以便它可以被重新调度和执行
                if (scheduledTaskRegistry.isCanceled(taskId)) {
                    // 重制任务状态已添加
                    scheduledTaskRegistry.resetCanceled(taskId);
                }
                // 发布事件
                taskEventPublisher.publishTaskEvent(taskMetaData);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void cancelTask(String taskId) {
        try {
            if (scheduledTaskRegistry.containsTask(taskId)) {
                scheduledTaskRegistry.cancelTask(taskId);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
