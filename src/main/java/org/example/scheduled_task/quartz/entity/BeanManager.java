package org.example.scheduled_task.quartz.entity;

import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.example.scheduled_task.entity.CoarseScheduledTaskMetaData;
import org.example.scheduled_task.quartz.task.ExecutedTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/22 20:19
 */
@Component
@Slf4j
public class BeanManager {
    @Resource
    private ApplicationContext applicationContext;

    /**
     * Spring 的DefaultListableBeanFactory使用一个内部的HashMap，
     * 其中key是beanName，而value是BeanDefinition。
     * 因此，每个唯一的beanName可以关联一个不同的BeanDefinition。
     * 但如果BeanDefinition是相同的（即，类相同并且配置相同），不需要重复注册它
     * <p>
     * 只注册一次BeanDefinition：对于同一个类，只注册一次BeanDefinition，无论要创建多少个实例。
     * 动态创建实例：根据需要动态创建每个实例
     *
     * @param classPath
     */
    public void ensureBeanDefinitionRegistered(String classPath) {
        try {
            ConfigurableApplicationContext configurableApplicationContext =
                    (ConfigurableApplicationContext) applicationContext;
            DefaultListableBeanFactory beanFactory =
                    (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
            // 用类路径作为定义名，以确保每种类只注册一次
            String beanDefinitionName = classPath;

            if (!beanFactory.containsBeanDefinition(beanDefinitionName)) {
                Class<?> clazz = null;

                clazz = Class.forName(classPath);

                BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(clazz)
                        .setScope(BeanDefinition.SCOPE_PROTOTYPE)
                        .getBeanDefinition();
                beanFactory.registerBeanDefinition(beanDefinitionName, beanDefinition);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public Object createAndRegisterBean(String beanName, String classPath) throws ClassNotFoundException {
        ensureBeanDefinitionRegistered(classPath);
        // 直接创建 Bean 实例，每次调用都会创建一个新的实例
        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

        Object instance = beanFactory.createBean(Class.forName(classPath));

        beanFactory.registerSingleton(beanName, instance);

        log.info("beanName: {}, 创建并注入容器: {}", beanName, instance);

        return instance;
    }

    public void removeBeanByName(String beanName) {
        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        if (beanFactory.containsSingleton(beanName)) {
            beanFactory.destroySingleton(beanName);
            log.info("beanName: {} 实例已销毁.", beanName);
            System.out.println();
        } else {
            log.info("beanName: {} 实例不存在.", beanName);
        }
    }

    public Object getBeanByName(String beanName) {
        return applicationContext.getBean(beanName);
    }


    /**
     * 递归调用
     *
     * @param task
     * @return
     * @throws IllegalAccessException
     */
    public static Map<String, Object> getProperties(Object task) throws IllegalAccessException {
        // 使用反射递归获取任务实例的自定义属性和依赖注入的属性
        Map<String, Object> properties = new HashMap<>();
        for (Field field : task.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(task);
            if (value != null && !isPrimitiveOrWrapper(value.getClass()) && !value.getClass().equals(String.class)) {
                // 递归处理对象属性
                properties.put(field.getName(), getProperties(value));
            } else {
                properties.put(field.getName(), value);
            }
        }
        return properties;
    }


    public static void setProperties(Object task, Map<String, Object> properties)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        AutowireCapableBeanFactory beanFactory = getBeanFactory();
        setPropertiesWithInjection(task, properties, beanFactory);
    }

    private static void setPropertiesWithInjection(Object task, Map<String, Object> properties, AutowireCapableBeanFactory beanFactory)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        // 使用反射递归设置任务实例的自定义属性和依赖注入的属性
        for (Field field : task.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = properties.get(field.getName());
            if (value != null && !isPrimitiveOrWrapper(field.getType()) && !field.getType().equals(String.class)) {
                // 如果属性是对象，递归设置属性
                Object nestedObject = field.getType().getDeclaredConstructor().newInstance();
                setPropertiesWithInjection(nestedObject, (Map<String, Object>) value, beanFactory);
                field.set(task, nestedObject);
                // 判断是否为依赖注入
                if (field.isAnnotationPresent(Autowired.class)) {
                    beanFactory.autowireBean(nestedObject);
                }
            } else {
                field.set(task, value);
            }
        }
    }

    private static AutowireCapableBeanFactory getBeanFactory() {
        // 获取Spring的AutowireCapableBeanFactory实例
        return SpringContextHolder.getApplicationContext().getAutowireCapableBeanFactory();
    }

    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Void.class);
    }


    public ExecutedTask createTaskInstance(CoarseScheduledTaskMetaData coarseScheduledTaskMetaData) {
        String taskId = coarseScheduledTaskMetaData.getTaskId();
        String taskClassPath = coarseScheduledTaskMetaData.getTaskClassPath();
        ExecutedTask instance = null;
        try {
            String beanName = "quartz_task:" + taskId;
            // 获取 Class 对象
            Class<?> clazz = Class.forName(taskClassPath);
            // 检查是否实现了指定接口
            if (ExecutedTask.class.isAssignableFrom(clazz)) {
                try {
                    instance = (ExecutedTask) createAndRegisterBean(beanName, taskClassPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException(taskClassPath + " 未实现接口 " + ExecutedTask.class.getName());
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("创建任务实例失败", e);
        }
    }

    public ExecutedTask assignValue(ExecutedTask instance, Map<String, Object> properties) {
        // 将 instance 的引用赋值给一个局部变量 finalInstance，
        // 可以避免在 lambda 表达式中直接使用 instance，并且无需将 instance 声明为 final。
        ExecutedTask finalInstance = instance;
        // 使用传入的properties参数给ExecutedTask实例赋值
        if (properties != null) {
            if (properties.entrySet() != null && !properties.entrySet().isEmpty()) {
                properties.forEach((key, value) -> {
                    try {
                        // 获取对应的字段
                        Field field = getField(finalInstance.getClass(), key);
                        if (field != null) {
                            field.setAccessible(true);
                            field.set(finalInstance, value);
                        } else {
                            throw new NoSuchFieldException("Field " + key + " not found in class " + finalInstance.getClass().getName());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("设置属性 " + key + " 失败", e);
                    }
                });
            }
        }
        return instance;
    }

    // 辅助方法，用于在类层次结构中查找字段
    public Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

}
