create table scheduled_task
(
    id              bigint auto_increment comment '主键'
        primary key,
    task_id         varbinary(255) null comment '用户输入的唯一标识符',
    task_name       varchar(255) null comment '任务名称',
    cron_expression varchar(255) null,
    task_status     varchar(100) null,
    deleted         tinyint null,
    constraint scheduled_task_pk_2
        unique (id)
);

create table task_class_dependencies
(
    id                 bigint auto_increment
        primary key,
    bean_type          text null comment '依赖注入的属性的类型',
    bean_name          text null comment '依赖注入的属性的在容器的名字',
    task_class_info_id bigint null comment 'task_class_info外键',
    deleted            tinyint null

);

create table task_class_info
(
    id              bigint auto_increment
        primary key,
    task_id         varchar(255) null comment '任务id，外键关联',
    task_class_path varchar(255) null comment '类路径',
    deleted         tinyint null,
    constraint task_properties_pk_2
        unique (id)
);

create table task_class_properties
(
    id                 int auto_increment comment '主键'
        primary key,
    property_name      varchar(255) null comment '属性名',
    property_value     text null comment '属性值',
    task_class_info_id bigint null comment 'task_class_info外键',
    deleted            tinyint null

);

