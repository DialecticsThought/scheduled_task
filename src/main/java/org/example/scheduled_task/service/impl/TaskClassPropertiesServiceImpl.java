package org.example.scheduled_task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.scheduled_task.entity.TaskClassProperties;
import org.example.scheduled_task.mapper.TaskClassPropertiesMapper;
import org.example.scheduled_task.service.TaskClassPropertiesService;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/23 16:03
 */
@Service
public class TaskClassPropertiesServiceImpl
        extends ServiceImpl<TaskClassPropertiesMapper, TaskClassProperties>
        implements TaskClassPropertiesService {
}
