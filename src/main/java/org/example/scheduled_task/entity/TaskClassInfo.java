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
 * @Data 2024/6/22 15:48
 */
@NoArgsConstructor
@Data
@AllArgsConstructor
@TableName("task_class_info")
public class TaskClassInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 外键，关联ScheduledTask表的taskId
     */
    private String taskId;
    /**
     * 类路径
     */
    private String taskClassPath;
    /**
     * 删除标志位
     */
    private Integer deleted;
}
