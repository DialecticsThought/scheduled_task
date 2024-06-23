package org.example.scheduled_task.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/23 15:10
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CoarseScheduledTaskMetaData {
    /**
     * 任务id
     */
    private String taskId;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 任务调度策略
     */
    private String taskClassPath;
    /**
     * cron表达式
     */
    private String cronExpression;
    /**
     * 任务属性
     */
    private Map<String, Object> properties;
}
