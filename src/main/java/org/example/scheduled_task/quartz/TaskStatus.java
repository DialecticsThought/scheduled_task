package org.example.scheduled_task.quartz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/12 11:26
 */
@Getter
public enum TaskStatus {
    /**
     * 没有状态
     */
    NONE("NONE"),
    /**
     * 添加
     */
    ADDED("ADDED"),
    /**
     * 执行
     */
    EXECUTED("EXECUTED"),
    /**
     * 取消
     */
    CANCELED("CANCELED"),
    /**
     * 删除
     */
    DELETED("DELETED");

    private final String status;

    TaskStatus(String status) {
        this.status = status;
    }

    @JsonCreator
    public static TaskStatus forValue(String value) {
        for (TaskStatus taskStatus : TaskStatus.values()) {
            if (taskStatus.getStatus().equals(value)) {
                return taskStatus;
            }
        }
        throw new IllegalArgumentException("Unknown enum type " + value);
    }

    @JsonValue
    public String toValue() {
        return this.status;
    }
}
