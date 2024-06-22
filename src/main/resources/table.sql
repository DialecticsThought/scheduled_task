create table scheduled_task
(
    id              bigint auto_increment comment '主键',
    task_id         varbinary(255) null comment '用户输入的唯一标识符',
    task_name       varchar(255) null comment '人物名称',
    task_class_path varchar(300) null comment '任务的类路径',
    constraint scheduled_task_pk
        primary key (id),
    constraint scheduled_task_pk_2
        unique (id)
);
