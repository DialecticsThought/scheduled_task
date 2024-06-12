package org.example.scheduled_task.quartz.factory;

import jakarta.annotation.Resource;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

/**
 * @author jiahao.liu
 * @description 该类的作用：创建job 并 注入Spring管理
 * @date 2024/05/23 16:03
 */
@Component
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

    @Resource
    private AutowireCapableBeanFactory beanFactory;

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {

        Object jobInstance = super.createJobInstance(bundle);

        beanFactory.autowireBean(jobInstance);

        return jobInstance;
    }
}
