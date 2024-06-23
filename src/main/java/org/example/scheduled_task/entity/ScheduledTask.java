package org.example.scheduled_task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/22 9:41
 */
@NoArgsConstructor
@Data
@AllArgsConstructor
@TableName("scheduled_task")
public class ScheduledTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String taskName;

    private String cronExpression;

    private String taskStatus;

    private Integer deleted;
}
