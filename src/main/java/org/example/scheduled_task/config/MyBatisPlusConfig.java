package org.example.scheduled_task.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @Description
 * @Author veritas
 * @Data 2024/6/23 16:16
 */
@Configuration
@MapperScan("org.example.scheduled_task.mapper")
public class MyBatisPlusConfig {
}
