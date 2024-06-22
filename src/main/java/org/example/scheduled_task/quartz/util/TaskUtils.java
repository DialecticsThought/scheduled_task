package org.example.scheduled_task.quartz.util;

import org.example.scheduled_task.entity.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/22 17:25
 */
public class TaskUtils {
    /**
     * 递归调用
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


    public static void setProperties(Object task, Map<String, Object> properties) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        AutowireCapableBeanFactory beanFactory = getBeanFactory();
        setPropertiesWithInjection(task, properties, beanFactory);
    }

    private static void setPropertiesWithInjection(Object task, Map<String, Object> properties, AutowireCapableBeanFactory beanFactory) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
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
}
