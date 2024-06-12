package org.example.scheduled_task.quartz;


import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiahao.liu
 * @description 注册中心
 * @date 2024/05/23 15:42
 */
@Component
public class ScheduledTaskRegistry {

    private final Map<String, ScheduledTaskMetaData<?>> taskMap = new ConcurrentHashMap<>();

    public ScheduledTaskMetaData<?> getTaskWithStrategy(String taskName) {
        return taskMap.get(taskName);
    }

    public void registerTask(String taskName, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        taskMap.put(taskName, scheduledTaskMetaData);
    }
}
