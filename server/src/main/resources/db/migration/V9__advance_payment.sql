create table advance_payment (
    id bigint primary key auto_increment,
    person_id bigint,
    name varchar(64) not null,
    id_card_no varchar(32) not null,
    phone varchar(32),
    amount decimal(18, 2) not null,
    advance_time datetime not null,
    advance_method varchar(32) not null,
    reason varchar(255) not null,
    remark varchar(255),
    created_by bigint not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    deleted tinyint not null default 0,
    key idx_advance_created_by (created_by),
    key idx_advance_time (advance_time),
    key idx_advance_person (name, id_card_no)
);

insert into sys_permission(code, name, type, menu_path, sort_no) values
('advance:view', '平时预支', 'MENU', '/advances', 15),
('advance:create', '新增预支', 'BUTTON', null, 16),
('advance:update', '编辑预支', 'BUTTON', null, 17),
('advance:delete', '删除预支', 'BUTTON', null, 18);
