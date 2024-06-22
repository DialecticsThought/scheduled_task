package org.example.scheduled_task.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.example.scheduled_task.quartz.TaskStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;

/**
 * @Description
 * @Author jiahao.liu
 * @Data 2024/6/21 22:35
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        //大多数情况，都是选用<String, Object>
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 使用StringRedisSerializer序列化和反序列化Redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        // 使用JSON的序列化对象，对数据key和value进行序列化转换
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        //ObjectMapper是Jackson的一个工作类，顾名思义他的作用是将JSON映射到Java对象即反序列化，或将Java对象映射到JSON即序列化
        ObjectMapper mapper = new ObjectMapper();
        // 设置序列化时的可见性，第一个参数是选择序列化哪些属性，比如时序列化setter?还是filed?h第二个参数是选择哪些修饰符权限的属性来序列化，比如private或者public，这里的any是指对所有权限修饰的属性都可见(可序列化)
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 设置多态类型验证器
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        // 启用默认的类型信息处理，以便处理多态对象
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        // 配置ObjectMapper处理枚举类型
        mapper.findAndRegisterModules();

        jackson2JsonRedisSerializer.setObjectMapper(mapper);
        // 设置RedisTemplate模板的序列化方式为jacksonSeial
        //redisTemplate.setDefaultSerializer(jackson2JsonRedisSerializer);
        // 设置RedisTemplate模板的序列化方式为Jackson序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }




}
