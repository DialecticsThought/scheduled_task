package org.example.scheduled_task.quartz.registry;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.scheduled_task.entity.ScheduledTask;
import org.example.scheduled_task.entity.TaskClassDependencies;
import org.example.scheduled_task.entity.TaskClassInfo;

import org.example.scheduled_task.entity.TaskClassProperties;
import org.example.scheduled_task.mapper.ScheduledTaskMapper;
import org.example.scheduled_task.mapper.TaskClassDependenciesMapper;
import org.example.scheduled_task.mapper.TaskClassInfoMapper;
import org.example.scheduled_task.mapper.TaskClassPropertiesMapper;
import org.example.scheduled_task.quartz.TaskStatus;
import org.example.scheduled_task.quartz.bridge.ScheduledTaskMetaData;
import org.example.scheduled_task.quartz.entity.BeanManager;
import org.example.scheduled_task.quartz.strategy.cron.CronScheduleStrategy;
import org.example.scheduled_task.quartz.task.ExecutedTask;
import org.example.scheduled_task.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.example.scheduled_task.quartz.TaskStatus.DELETED;


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
    private ScheduledTaskMapper scheduledTaskMapper;
    @Resource
    private TaskClassInfoMapper taskClassInfoMapper;
    @Resource
    private TaskClassPropertiesMapper taskClassPropertiesMapper;
    @Resource
    private TaskClassDependenciesMapper taskClassDependenciesMapper;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private BeanManager beanManager;
    @Resource
    private TaskService taskService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return loadTaskWithDependencies(taskId);
    }

    @Override
    public void registerTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        // ScheduledTask
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setTaskId(scheduledTaskMetaData.getTaskId());
        scheduledTask.setTaskName(scheduledTaskMetaData.getTaskName());
        scheduledTask.setCronExpression(((CronScheduleStrategy) scheduledTaskMetaData.getScheduleStrategy()).getCronExpression());
        scheduledTask.setTaskStatus(TaskStatus.ADDED.toValue());
        scheduledTask.setDeleted(0);
        scheduledTaskMapper.insert(scheduledTask);

        // TaskClassInfo
        TaskClassInfo taskClassInfo = new TaskClassInfo();
        taskClassInfo.setTaskId(scheduledTaskMetaData.getTaskId());
        taskClassInfo.setTaskClassPath(scheduledTaskMetaData.getExecutedTask().getClass().getName());
        taskClassInfo.setDeleted(0);
        taskClassInfoMapper.insert(taskClassInfo);

        // TaskClassProperties
        if (scheduledTaskMetaData.getProperties() != null) {
            for (Map.Entry<String, Object> entry : scheduledTaskMetaData.getProperties().entrySet()) {
                TaskClassProperties taskClassProperties = new TaskClassProperties();
                taskClassProperties.setPropertyName(entry.getKey());
                taskClassProperties.setPropertyValue(entry.getValue().toString());
                taskClassProperties.setDeleted(0);
                taskClassProperties.setTaskClassInfoId(taskClassInfo.getId());
                taskClassPropertiesMapper.insert(taskClassProperties);
            }
        }
        // TaskClassDependencies
        for (Field field : scheduledTaskMetaData.getExecutedTask().getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Resource.class) || field.isAnnotationPresent(Autowired.class)) {
                TaskClassDependencies taskClassDependencies = new TaskClassDependencies();
                taskClassDependencies.setBeanType(field.getType().getName());
                taskClassDependencies.setBeanName(field.getName());
                taskClassDependencies.setDeleted(0);
                taskClassDependencies.setTaskClassInfoId(taskClassInfo.getId());
                taskClassDependenciesMapper.insert(taskClassDependencies);
            }
        }
    }

    @Override
    public void markExecute(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ;
        ScheduledTask scheduledTask = scheduledTaskMapper.selectOne(queryWrapper);

        // 如果任务实体存在并且状态不是已执行，则更新状态为已执行
        if (scheduledTask != null && !TaskStatus.EXECUTED.toValue().equals(scheduledTask.getTaskStatus())) {
            scheduledTask.setTaskStatus(TaskStatus.EXECUTED.toValue());
            scheduledTaskMapper.updateById(scheduledTask);
        }
    }

    @Override
    public void removeTask(String taskId) {
        // 根据taskId删除ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskMapper.selectOne(queryWrapper);
        if (scheduledTask != null) {
            scheduledTask.setDeleted(1);
            scheduledTaskMapper.updateById(scheduledTask);

            removeTaskFromSpringContainer(taskId);
        }

        QueryWrapper<TaskClassInfo> classInfoQueryWrapper = new QueryWrapper<>();
        classInfoQueryWrapper.eq("task_id", taskId).eq("deleted", 0);
        TaskClassInfo taskClassInfo = taskClassInfoMapper.selectOne(classInfoQueryWrapper);

        if (taskClassInfo != null) {
            taskClassInfo.setDeleted(1);
            taskClassInfoMapper.updateById(taskClassInfo);

            QueryWrapper<TaskClassProperties> propertiesQueryWrapper = new QueryWrapper<>();
            propertiesQueryWrapper.eq("task_class_info_id", taskClassInfo.getId());
            List<TaskClassProperties> properties = taskClassPropertiesMapper.selectList(propertiesQueryWrapper);
            for (TaskClassProperties property : properties) {
                property.setDeleted(1);
                taskClassPropertiesMapper.updateById(property);
            }

            QueryWrapper<TaskClassDependencies> dependenciesQueryWrapper = new QueryWrapper<>();
            dependenciesQueryWrapper.eq("task_class_info_id", taskClassInfo.getId());
            List<TaskClassDependencies> dependencies = taskClassDependenciesMapper.selectList(dependenciesQueryWrapper);
            for (TaskClassDependencies dependency : dependencies) {
                dependency.setDeleted(1);
                taskClassDependenciesMapper.updateById(dependency);
            }
        }
    }

    @Override
    public boolean containsTask(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否存在
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        return scheduledTaskMapper.selectOne(queryWrapper) != null;
    }

    @Override
    public void cancelTask(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        ScheduledTask scheduledTask = scheduledTaskMapper.selectOne(queryWrapper);

        // 如果任务实体存在，则更新状态为已取消
        if (scheduledTask != null) {
            scheduledTask.setTaskStatus(TaskStatus.CANCELED.toValue());
            scheduledTaskMapper.updateById(scheduledTask);
        }
    }

    @Override
    public void deleteTask(String taskId) {
        cancelTask(taskId);
        removeTask(taskId);
    }

    @Override
    public boolean isCanceled(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否已取消
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskMapper.selectOne(queryWrapper);

        return scheduledTask != null && TaskStatus.CANCELED.toValue().equals(scheduledTask.getTaskStatus());
    }

    @Override
    public boolean isDeleted(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否已删除
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 1);
        return scheduledTaskMapper.selectOne(queryWrapper) == null;
    }

    @Override
    public void resetCanceled(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskMapper.selectOne(queryWrapper);

        // 如果任务实体存在且状态为已取消，则重置状态为已添加
        if (scheduledTask != null && TaskStatus.CANCELED.toValue().equals(scheduledTask.getTaskStatus())) {
            scheduledTask.setTaskStatus(TaskStatus.ADDED.toValue());
            scheduledTaskMapper.updateById(scheduledTask);
        }
    }

    private void removeTaskFromSpringContainer(String taskId) {
        String beanName = taskId + ":quartz_task";
        beanManager.removeBeanByName(beanName);
    }


    private Object convertToFieldType(Field field, String value) {
        Class<?> fieldType = field.getType();
        if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(value);
        } else if (fieldType == long.class || fieldType == Long.class) {
            return Long.parseLong(value);
        } else if (fieldType == double.class || fieldType == Double.class) {
            return Double.parseDouble(value);
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (fieldType == String.class) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported field type: " + fieldType.getName());
    }

    private ExecutedTask<?> instantiateAndInject(TaskClassInfo taskClassInfo, List<TaskClassProperties> properties, List<TaskClassDependencies> dependencies) {
        try {
            Class<?> taskClass = Class.forName(taskClassInfo.getTaskClassPath());
            ExecutedTask<?> taskInstance = (ExecutedTask<?>) taskClass.getDeclaredConstructor().newInstance();

            for (TaskClassDependencies dependency : dependencies) {
                Object bean = applicationContext.getBean(dependency.getBeanName());
                Field field = taskClass.getDeclaredField(dependency.getBeanName());
                field.setAccessible(true);
                field.set(taskInstance, bean);
            }

            for (TaskClassProperties property : properties) {
                Field field = taskClass.getDeclaredField(property.getPropertyName());
                field.setAccessible(true);
                Object value = convertToFieldType(field, property.getPropertyValue());
                field.set(taskInstance, value);
            }

            applicationContext.getAutowireCapableBeanFactory().autowireBean(taskInstance);
            applicationContext.getAutowireCapableBeanFactory().initializeBean(taskInstance, taskClassInfo.getTaskId());

            return taskInstance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate or inject task dependencies", e);
        }
    }

    public ScheduledTaskMetaData<?> loadTaskWithDependencies(String taskId) {
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskMapper.selectOne(queryWrapper);

        if (scheduledTask == null) {
            throw new RuntimeException("Task not found or marked as deleted");
        }

        QueryWrapper<TaskClassInfo> classInfoQueryWrapper = new QueryWrapper<>();
        classInfoQueryWrapper.eq("task_id", taskId).eq("deleted", 0);
        TaskClassInfo taskClassInfo = taskClassInfoMapper.selectOne(classInfoQueryWrapper);

        QueryWrapper<TaskClassProperties> propQueryWrapper = new QueryWrapper<>();
        propQueryWrapper.eq("task_class_info_id", taskClassInfo.getId()).eq("deleted", 0);
        List<TaskClassProperties> properties = taskClassPropertiesMapper.selectList(propQueryWrapper);

        QueryWrapper<TaskClassDependencies> depQueryWrapper = new QueryWrapper<>();
        depQueryWrapper.eq("task_class_info_id", taskClassInfo.getId()).eq("deleted", 0);
        List<TaskClassDependencies> dependencies = taskClassDependenciesMapper.selectList(depQueryWrapper);

        ExecutedTask<?> taskInstance = instantiateAndInject(taskClassInfo, properties, dependencies);

        ScheduledTaskMetaData metaData = new ScheduledTaskMetaData<>();
        metaData.setTaskId(scheduledTask.getTaskId());
        metaData.setTaskName(scheduledTask.getTaskName());
        metaData.setScheduleStrategy(new CronScheduleStrategy(scheduledTask.getCronExpression()));
        metaData.setExecutedTask(taskInstance);

        return metaData;
    }

    public void initializeTasksFromDatabase() {
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        queryWrapper.ne("task_status", DELETED.toValue());
        List<ScheduledTask> allTasks = scheduledTaskMapper.selectList(queryWrapper);
        for (ScheduledTask scheduledTask : allTasks) {
            String taskId = scheduledTask.getTaskId();
            ScheduledTaskMetaData<?> metaData = loadTaskWithDependencies(taskId);
            // Register the task in memory, possibly using a service or registry
        }
    }
}
