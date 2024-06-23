package org.example.scheduled_task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.scheduled_task.entity.ScheduledTask;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/22 9:44
 */
@Mapper
public interface ScheduledTaskMapper extends BaseMapper<ScheduledTask> {
}
