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
import org.example.scheduled_task.service.*;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.example.scheduled_task.quartz.TaskStatus.*;


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
@Component
public class DatabaseTaskRegistry implements ScheduledTaskRegistry {
    @Resource
    private ScheduledTaskService scheduledTaskService;

    @Resource
    private TaskClassInfoService taskClassInfoService;

    @Resource
    private TaskClassPropertiesService taskClassPropertiesService;

    @Resource
    private TaskClassDependenciesService taskClassDependenciesService;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private BeanManager beanManager;

    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;

    @Override
    public ScheduledTaskMetaData<?> getScheduledTaskMetaData(String taskId) {
        return loadTaskWithDependencies(taskId);
    }

    @Override
    @Transactional
    public void registerTask(ScheduledTaskMetaData<?> scheduledTaskMetaData) {
        // ScheduledTask
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setTaskId(scheduledTaskMetaData.getTaskId());
        scheduledTask.setTaskName(scheduledTaskMetaData.getTaskName());
        scheduledTask.setCronExpression(((CronScheduleStrategy) scheduledTaskMetaData.getScheduleStrategy()).getCronExpression());
        scheduledTask.setTaskStatus(TaskStatus.ADDED.toValue());
        scheduledTask.setDeleted(0);
        scheduledTaskService.save(scheduledTask);
        // TaskClassInfo
        TaskClassInfo taskClassInfo = new TaskClassInfo();
        taskClassInfo.setTaskId(scheduledTaskMetaData.getTaskId());
        taskClassInfo.setTaskClassPath(scheduledTaskMetaData.getExecutedTask().getClass().getName());
        taskClassInfo.setDeleted(0);
        taskClassInfoService.save(taskClassInfo);
        // TaskClassProperties
        if (scheduledTaskMetaData.getProperties() != null) {
            Set<Map.Entry<String, Object>> entries = scheduledTaskMetaData.getProperties().entrySet();
            if (entries != null && !entries.isEmpty()) {
                for (Map.Entry<String, Object> entry : entries) {
                    TaskClassProperties taskClassProperties = new TaskClassProperties();
                    taskClassProperties.setPropertyName(entry.getKey());
                    taskClassProperties.setPropertyValue(entry.getValue().toString());
                    taskClassProperties.setDeleted(0);
                    taskClassProperties.setTaskClassInfoId(taskClassInfo.getId());
                    taskClassPropertiesService.save(taskClassProperties);
                }
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
                taskClassDependenciesService.save(taskClassDependencies);
            }
        }
    }

    @Override
    @Transactional
    public void markExecute(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);

        ScheduledTask scheduledTask = scheduledTaskService.getOne(queryWrapper);

        // 如果任务实体存在并且状态不是已执行，则更新状态为已执行
        if (scheduledTask != null && !TaskStatus.EXECUTED.toValue().equals(scheduledTask.getTaskStatus())) {
            scheduledTask.setTaskStatus(TaskStatus.EXECUTED.toValue());
            scheduledTaskService.updateById(scheduledTask);
        }
    }

    @Override
    @Transactional
    public void removeTask(String taskId) {
        // 根据taskId删除ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskService.getOne(queryWrapper);
        if (scheduledTask != null) {
            scheduledTask.setDeleted(1);
            scheduledTask.setTaskStatus(DELETED.toValue());
            scheduledTaskService.updateById(scheduledTask);

            removeTaskFromSpringContainer(taskId);
        }

        QueryWrapper<TaskClassInfo> classInfoQueryWrapper = new QueryWrapper<>();
        classInfoQueryWrapper.eq("task_id", taskId).eq("deleted", 0);
        TaskClassInfo taskClassInfo = taskClassInfoService.getOne(classInfoQueryWrapper);

        if (taskClassInfo != null) {
            taskClassInfo.setDeleted(1);
            taskClassInfoService.updateById(taskClassInfo);

            QueryWrapper<TaskClassProperties> propertiesQueryWrapper = new QueryWrapper<>();
            propertiesQueryWrapper.eq("task_class_info_id", taskClassInfo.getId());
            List<TaskClassProperties> properties = taskClassPropertiesService.list(propertiesQueryWrapper);
            for (TaskClassProperties property : properties) {
                property.setDeleted(1);
                taskClassPropertiesService.updateById(property);
            }

            QueryWrapper<TaskClassDependencies> dependenciesQueryWrapper = new QueryWrapper<>();
            dependenciesQueryWrapper.eq("task_class_info_id", taskClassInfo.getId());
            List<TaskClassDependencies> dependencies = taskClassDependenciesService.list(dependenciesQueryWrapper);
            for (TaskClassDependencies dependency : dependencies) {
                dependency.setDeleted(1);
                taskClassDependenciesService.updateById(dependency);
            }
        }
    }

    @Override
    public boolean containsTask(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否存在
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        return scheduledTaskService.getOne(queryWrapper) != null;
    }

    @Override
    @Transactional
    public void cancelTask(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskService.getOne(queryWrapper);

        // 如果任务实体存在，则更新状态为已取消
        if (scheduledTask != null) {
            scheduledTask.setTaskStatus(TaskStatus.CANCELED.toValue());
            scheduledTaskService.updateById(scheduledTask);

            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            //TODO 只有执行状态下面 quartz才会有任务信息对象
            try {
                //scheduler.deleteJob(new JobKey("quartz_task:" + taskId));
                scheduler.deleteJob(new JobKey(taskId));
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        cancelTask(taskId);
        removeTask(taskId);
    }

    @Override
    public boolean isCanceled(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否已取消
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskService.getOne(queryWrapper);

        return scheduledTask != null && TaskStatus.CANCELED.toValue().equals(scheduledTask.getTaskStatus());
    }

    @Override
    public boolean isDeleted(String taskId) {
        // 根据taskId查询ScheduledTask实体，判断任务是否已删除
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 1);
        return scheduledTaskService.getOne(queryWrapper) == null;
    }

    @Override
    @Transactional
    public void resetCanceled(String taskId) {
        // 根据taskId查询ScheduledTask实体
        QueryWrapper<ScheduledTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("deleted", 0);
        ScheduledTask scheduledTask = scheduledTaskService.getOne(queryWrapper);

        // 如果任务实体存在且状态为已取消，则重置状态为已添加
        if (scheduledTask != null && TaskStatus.CANCELED.toValue().equals(scheduledTask.getTaskStatus())) {
            scheduledTask.setTaskStatus(TaskStatus.ADDED.toValue());
            scheduledTaskService.updateById(scheduledTask);
        }
    }

    private void removeTaskFromSpringContainer(String taskId) {
        String beanName = "quartz_task:" + taskId;
        beanManager.removeBeanByName(beanName);
    }


    private Object convertToFieldType(Field field, String value) {
        Class<?> fieldType = field.getType();
        if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(value);
        } else if (fieldType == long.class || fieldType == Long.class) {
            return Long.parseLong(value);
        } else if (fieldType == float.class || fieldType == Field.class) {
            return Float.parseFloat(value);
        } else if (fieldType == double.class || fieldType == Double.class) {
            return Double.parseDouble(value);
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (fieldType == String.class) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported field type: " + fieldType.getName());
    }


    private ExecutedTask<?> instantiateAndInject(TaskClassInfo taskClassInfo,
                                                 List<TaskClassProperties> properties,
                                                 List<TaskClassDependencies> dependencies) {
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
        ScheduledTask scheduledTask = scheduledTaskService.getOne(queryWrapper);

        if (scheduledTask == null) {
            throw new RuntimeException("任务没有找到 或者 被标记为已删除");
        }

        QueryWrapper<TaskClassInfo> classInfoQueryWrapper = new QueryWrapper<>();
        classInfoQueryWrapper.eq("task_id", taskId).eq("deleted", 0);
        TaskClassInfo taskClassInfo = taskClassInfoService.getOne(classInfoQueryWrapper);

        QueryWrapper<TaskClassProperties> propQueryWrapper = new QueryWrapper<>();
        propQueryWrapper.eq("task_class_info_id", taskClassInfo.getId()).eq("deleted", 0);
        List<TaskClassProperties> properties = taskClassPropertiesService.list(propQueryWrapper);

        QueryWrapper<TaskClassDependencies> depQueryWrapper = new QueryWrapper<>();
        depQueryWrapper.eq("task_class_info_id", taskClassInfo.getId()).eq("deleted", 0);
        List<TaskClassDependencies> dependencies = taskClassDependenciesService.list(depQueryWrapper);

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
        List<ScheduledTask> allTasks = scheduledTaskService.list(queryWrapper);
        for (ScheduledTask scheduledTask : allTasks) {
            String taskId = scheduledTask.getTaskId();
            String taskStatus = scheduledTask.getTaskStatus();
            if (EXECUTED.toValue().equals(taskStatus) || ADDED.toValue().equals(taskStatus)) {
                ScheduledTaskMetaData<?> metaData = loadTaskWithDependencies(taskId);
            }
            // TODO ....
        }
    }
}
