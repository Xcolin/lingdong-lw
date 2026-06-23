create table sys_permission (
    id bigint primary key auto_increment,
    code varchar(64) not null,
    name varchar(64) not null,
    type varchar(16) not null,
    menu_path varchar(128),
    sort_no int not null default 0,
    created_at datetime not null default current_timestamp,
    unique key uk_sys_permission_code (code)
);

create table sys_user_permission (
    id bigint primary key auto_increment,
    user_id bigint not null,
    permission_id bigint not null,
    unique key uk_sys_user_permission (user_id, permission_id),
    key idx_sys_user_permission_user (user_id),
    key idx_sys_user_permission_permission (permission_id)
);

insert into sys_permission(code, name, type, menu_path, sort_no) values
('batch:view', '工资批次', 'MENU', '/batches', 10),
('batch:create', '新建工资批次', 'BUTTON', null, 11),
('batch:update', '编辑工资批次', 'BUTTON', null, 12),
('batch:delete', '删除工资批次', 'BUTTON', null, 13),
('batch:export', '导出工资模板', 'BUTTON', null, 14),
('person:view', '人员信息库', 'MENU', '/persons', 20),
('person:create', '新增人员', 'BUTTON', null, 21),
('person:update', '编辑人员', 'BUTTON', null, 22),
('person:delete', '删除人员', 'BUTTON', null, 23),
('person:enable', '启用停用人员', 'BUTTON', null, 24),
('unit:view', '单位信息库', 'MENU', '/units', 30),
('unit:create', '新增单位', 'BUTTON', null, 31),
('unit:update', '编辑单位', 'BUTTON', null, 32),
('unit:delete', '删除单位', 'BUTTON', null, 33),
('unit:enable', '启用停用单位', 'BUTTON', null, 34),
('export:view', '导出记录', 'MENU', '/exports', 40),
('log:view', '操作日志', 'MENU', '/logs', 50),
('user:view', '用户管理', 'MENU', '/users', 60),
('user:create', '新增用户', 'BUTTON', null, 61),
('user:update', '编辑用户', 'BUTTON', null, 62),
('user:enable', '启用停用用户', 'BUTTON', null, 63),
('user:password', '重置密码', 'BUTTON', null, 64),
('user:permission', '用户授权', 'BUTTON', null, 65);
