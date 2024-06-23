package org.example.scheduled_task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.scheduled_task.entity.ScheduledTask;
import org.example.scheduled_task.mapper.ScheduledTaskMapper;
import org.example.scheduled_task.service.ScheduledTaskService;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/23 16:00
 */
@Service
public class ScheduledTaskServiceImpl
        extends ServiceImpl<ScheduledTaskMapper, ScheduledTask>
        implements ScheduledTaskService {
}
