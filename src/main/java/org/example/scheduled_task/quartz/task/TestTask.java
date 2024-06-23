package org.example.scheduled_task.quartz.task;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Scope;



/**
 * @author jiahao.liu
 * @description
 * @date 2024/06/11 19:12
 */
@Scope("prototype")// 所有任务类都要加上
public class TestTask implements ExecutedTask<Void> {

    @Resource
    private Test test;

    @Override
    public Void execute() {
        test.test();
        return null;
    }
}
