package org.example.scheduled_task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.scheduled_task.entity.TaskClassInfo;
import org.example.scheduled_task.mapper.TaskClassInfoMapper;
import org.example.scheduled_task.service.TaskClassInfoService;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/23 16:02
 */
@Service
public class TaskClassInfoServiceImpl
        extends ServiceImpl<TaskClassInfoMapper, TaskClassInfo>
        implements TaskClassInfoService {
}
