package org.example.scheduled_task.quartz.registry;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.scheduled_task.entity.ScheduledTask;
import org.example.scheduled_task.entity.TaskProperties;

import org.example.scheduled_task.mapper.ScheduledTaskMapper;
import org.example.scheduled_task.mapper.TaskPropertiesMapper;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.entity.BeanManager;
import org.example.scheduled_task.quartz.strategy.cron.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.ExecutedTask;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.util.Map;


/**
 * @Description TODO
 * 创建任务对象的过程
 * 1.转换和存储任务对象的属性：
 * 2.在 registerTask 方法中，将任务对象的自定义属性和类路径存储到数据库中。
 * 3.使用反射获取任务对象的所有自定义属性，并将它们序列化为 JSON 字符串存储在 TaskProperties 表中。
 * 恢复任务对象的过程：
 * 1.在 getScheduledTaskMetaData 方法中，从数据库中读取任务对象的类路径和属性。
 * 2.动态加载任务类，实例化任务对象。
 * 3.使用 Spring 容器注入依赖到任务对象中。
 * 4.使用反射将存储的自定义属性设置回任务对象。
 * @Author veritas
 * @Data 2024/6/22 15:17
 */
public class DatabaseTaskRegistry implements ScheduledTaskRegistry {
    @Resource
    private ScheduledTaskMapper taskMapper;

    @Resource
    private TaskPropertiesMapper taskPropertiesMapper;

    @Resource
    private AutowireCapableBeanFactory beanFactory;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        // 根据taskId从数据库中查询任务
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        ScheduledTask taskEntity = taskMapper.selectOne(queryWrapper);

        // 根据taskId从task_properties表中查询任务属性
        QueryWrapper<TaskProperties> propertiesQueryWrapper = new QueryWrapper<>();
        propertiesQueryWrapper.eq("task_id", taskId);
        TaskProperties taskProperties = taskPropertiesMapper.selectOne(propertiesQueryWrapper);

        // 如果任务实体和任务属性都存在，则转换为ScheduledTaskMetaData对象
        if (taskEntity != null && taskProperties != null) {
            return convertToMetaData(taskEntity, taskProperties);
        }
        return null;
    }

    @Override
    public void registerTask(String taskId, ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        // 将ScheduledTaskMetaData对象转换为ScheduledTask实体并插入到数据库
        ScheduledTask taskEntity = convertToEntity(scheduledTaskMetaData);
        taskEntity.setTaskStatus(TaskStatus.ADDED.toValue());
        taskMapper.insert(taskEntity);

        // 创建TaskProperties对象并插入到数据库
        TaskProperties taskProperties = new TaskProperties();
        taskProperties.setTaskId(taskId);
        try {
            if (scheduledTaskMetaData.getExecutedTask() != null) {
                // 获取任务自定义属性和依赖注入的属性
                String taskClassPath = scheduledTaskMetaData.getExecutedTask().getClass().getName();
                taskProperties.setTaskClassPath(taskClassPath);

                // 获取任务属性
                Map<String, Object> properties = BeanManager.getProperties(scheduledTaskMetaData.getExecutedTask());
                taskProperties.setProperties(objectMapper.writeValueAsString(properties));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        taskPropertiesMapper.insert(taskProperties);
    }

    @Override
    public void markExecute(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        ScheduledTask taskEntity = taskMapper.selectOne(queryWrapper);

        // 如果任务实体存在并且状态不是已执行，则更新状态为已执行
        if (taskEntity != null && !TaskStatus.EXECUTED.toValue().equals(taskEntity.getTaskStatus())) {
            taskEntity.setTaskStatus(TaskStatus.EXECUTED.toValue());
            taskMapper.updateById(taskEntity);
        }
    }

    @Override
    public void removeTask(String taskId) {
        // 根据taskId删除ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        taskMapper.delete(queryWrapper);

        // 根据taskId删除TaskProperties实体
        QueryWrapper<TaskProperties> propertiesQueryWrapper = new QueryWrapper<>();
        propertiesQueryWrapper.eq("task_id", taskId);
        taskPropertiesMapper.delete(propertiesQueryWrapper);
    }

    @Override
    public boolean containsTask(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否存在
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        return taskMapper.selectOne(queryWrapper) != null;
    }

    @Override
    public void cancelTask(String taskId)  {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        ScheduledTask taskEntity = taskMapper.selectOne(queryWrapper);

        // 如果任务实体存在，则更新状态为已取消
        if (taskEntity != null) {
            taskEntity.setTaskStatus(TaskStatus.CANCELED.toValue());
            taskMapper.updateById(taskEntity);
        }
    }

    @Override
    public void deleteTask(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        ScheduledTask taskEntity = taskMapper.selectOne(queryWrapper);

        // 如果任务实体存在，则更新状态为已删除，并删除任务实体和属性
        if (taskEntity != null) {
            taskEntity.setTaskStatus(TaskStatus.DELETED.toValue());
            taskMapper.updateById(taskEntity);

            taskMapper.delete(queryWrapper);

            QueryWrapper<TaskProperties> propertiesQueryWrapper = new QueryWrapper<>();
            propertiesQueryWrapper.eq("task_id", taskId);
            taskPropertiesMapper.delete(propertiesQueryWrapper);
        }
    }

    @Override
    public boolean isCanceled(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否已取消
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        ScheduledTask taskEntity = taskMapper.selectOne(queryWrapper);

        return taskEntity != null && TaskStatus.CANCELED.toValue().equals(taskEntity.getTaskStatus());
    }

    @Override
    public boolean isDeleted(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否已删除
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        return taskMapper.selectOne(queryWrapper) == null;
    }

    @Override
    public void resetCanceled(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        ScheduledTask taskEntity = taskMapper.selectOne(queryWrapper);

        // 如果任务实体存在且状态为已取消，则重置状态为已添加
        if (taskEntity != null && TaskStatus.CANCELED.toValue().equals(taskEntity.getTaskStatus())) {
            taskEntity.setTaskStatus(TaskStatus.ADDED.toValue());
            taskMapper.updateById(taskEntity);
        }
    }

    private ScheduledTask convertToEntity(ScheduledTaskMetaData<?> metaData) {
        // 将ScheduledTaskMetaData对象转换为ScheduledTask实体
        ScheduledTask taskEntity = new ScheduledTask();
        taskEntity.setTaskId(metaData.getTaskId());
        taskEntity.setTaskName(metaData.getTaskName());
        taskEntity.setCronExpression(((CronScheduleStrategy) metaData.getScheduleStrategy()).getCronExpression());
        return taskEntity;
    }

    private ScheduledTaskMetaData<?> convertToMetaData(ScheduledTask taskEntity, TaskProperties taskProperties) {
        // 将ScheduledTask和TaskProperties对象转换为ScheduledTaskMetaData对象
        ScheduledTaskMetaData<?> metaData = new ScheduledTaskMetaData<>();
        metaData.setTaskId(taskEntity.getTaskId());
        metaData.setTaskName(taskEntity.getTaskName());
        metaData.setScheduleStrategy(new CronScheduleStrategy(taskEntity.getCronExpression()));

        try {
            // 动态加载任务类并实例化
            Class<?> taskClass = Class.forName(taskProperties.getTaskClassPath());
            ExecutedTask taskInstance = (ExecutedTask) taskClass.getDeclaredConstructor().newInstance();

            // 注入Spring容器中的依赖
            beanFactory.autowireBean(taskInstance);

            // 如果存在属性，则反序列化属性并设置到任务实例中
            if (taskProperties.getProperties() != null) {
                Map<String, Object> properties = objectMapper.readValue(taskProperties.getProperties(), Map.class);
                BeanManager.setProperties(taskInstance, properties);
                metaData.setProperties(properties);
            }

            metaData.setExecutedTask(taskInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return metaData;
    }
}
