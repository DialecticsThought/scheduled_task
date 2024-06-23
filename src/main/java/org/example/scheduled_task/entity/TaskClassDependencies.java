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
 * @Data 2024/6/23 13:50
 */
@NoArgsConstructor
@Data
@AllArgsConstructor
@TableName("task_class_dependencies")
public class TaskClassDependencies {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 外键，关联taskClassInfo表的id
     */
    private Long taskClassInfoId;
    /**
     * JSON字符串形式存储属性
     */
    private String beanName;
    /**
     *
     */
    private String beanType;
    /**
     * 删除标志位
     */
    private Integer deleted;
}
