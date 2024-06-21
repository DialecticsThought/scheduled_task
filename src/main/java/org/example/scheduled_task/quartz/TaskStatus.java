package org.example.scheduled_task.quartz;

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
    NONE,
    /**
     * 添加
     */
    ADDED,
    /**
     * 执行
     */
    EXECUTED,
    /**
     * 取消
     */
    CANCELED,
    /**
     * 删除
     */
    DELETED;
}
