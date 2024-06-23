package org.example.scheduled_task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.scheduled_task.entity.TaskClassDependencies;

import org.example.scheduled_task.mapper.TaskClassDependenciesMapper;
import org.example.scheduled_task.service.TaskClassDependenciesService;
import org.springframework.stereotype.Service;


/**
 * @Description
 * @Author veritas
 * @Data 2024/6/23 16:01
 */
@Service
public class TaskClassDependenciesServiceImpl
        extends ServiceImpl<TaskClassDependenciesMapper, TaskClassDependencies>
        implements TaskClassDependenciesService {
}
