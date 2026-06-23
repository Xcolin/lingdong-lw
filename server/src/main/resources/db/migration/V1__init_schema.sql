create table sys_user (
    id bigint primary key auto_increment,
    username varchar(64) not null,
    password_hash varchar(128) not null,
    display_name varchar(64) not null,
    enabled tinyint not null default 1,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_sys_user_username (username)
);

create table sys_role (
    id bigint primary key auto_increment,
    code varchar(32) not null,
    name varchar(64) not null,
    created_at datetime not null default current_timestamp,
    unique key uk_sys_role_code (code)
);

create table sys_user_role (
    id bigint primary key auto_increment,
    user_id bigint not null,
    role_id bigint not null,
    unique key uk_sys_user_role (user_id, role_id),
    key idx_sys_user_role_user (user_id),
    key idx_sys_user_role_role (role_id)
);

create table payee_person (
    id bigint primary key auto_increment,
    name varchar(64) not null,
    id_card_no varchar(32) not null,
    phone varchar(32),
    bank_account varchar(64) not null,
    account_name varchar(120) not null,
    bank_name varchar(160),
    bank_type varchar(64),
    bank_category varchar(64),
    cnaps_no varchar(32),
    created_by bigint not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    deleted tinyint not null default 0,
    unique key uk_payee_created_id_card (created_by, id_card_no),
    key idx_payee_created_by (created_by),
    key idx_payee_keyword (name, phone, bank_account)
);

create table paying_unit (
    id bigint primary key auto_increment,
    bank_account varchar(64) not null,
    account_name varchar(120) not null,
    bank_name varchar(160),
    bank_type varchar(64),
    bank_category varchar(64),
    cnaps_no varchar(32),
    created_by bigint not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    deleted tinyint not null default 0,
    unique key uk_unit_created_bank_account (created_by, bank_account),
    key idx_unit_created_by (created_by),
    key idx_unit_keyword (account_name, bank_account)
);

create table payroll_batch (
    id bigint primary key auto_increment,
    batch_name varchar(120) not null,
    pay_date date,
    default_summary varchar(32),
    remark varchar(255),
    total_people int not null default 0,
    total_amount decimal(18, 2) not null default 0.00,
    created_by bigint not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    deleted tinyint not null default 0,
    key idx_batch_created_by (created_by),
    key idx_batch_pay_date (pay_date)
);

create table payroll_batch_item (
    id bigint primary key auto_increment,
    batch_id bigint not null,
    person_id bigint,
    row_no int not null,
    name varchar(64) not null,
    id_card_no varchar(32) not null,
    phone varchar(32),
    bank_account varchar(64) not null,
    account_name varchar(120) not null,
    bank_name varchar(160),
    bank_type varchar(64),
    bank_category varchar(64),
    cnaps_no varchar(32),
    amount decimal(18, 2) not null,
    summary varchar(32),
    remark varchar(255),
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    key idx_batch_item_batch (batch_id),
    key idx_batch_item_person (person_id)
);

create table export_record (
    id bigint primary key auto_increment,
    batch_id bigint not null,
    paying_unit_id bigint not null,
    template_type varchar(32) not null,
    file_name varchar(255) not null,
    file_path varchar(500) not null,
    total_people int not null,
    total_amount decimal(18, 2) not null,
    created_by bigint not null,
    created_at datetime not null default current_timestamp,
    key idx_export_created_by (created_by),
    key idx_export_batch (batch_id)
);

create table operation_log (
    id bigint primary key auto_increment,
    operator_id bigint not null,
    operator_name varchar(64),
    action varchar(32) not null,
    module varchar(64) not null,
    business_id bigint,
    before_value text,
    after_value text,
    ip_address varchar(64),
    user_agent varchar(255),
    operated_at datetime not null default current_timestamp,
    key idx_log_operator_time (operator_id, operated_at),
    key idx_log_module_action (module, action)
);

insert into sys_role(code, name) values ('ADMIN', '管理员'), ('OPERATOR', '录入人员');
insert into sys_user(username, password_hash, display_name, enabled)
values ('admin', '$2a$10$br9mlS.XFIhbmwQALXeske3fmTwGz62VfsqL1CcfJZWvOJmDmkhf6', '系统管理员', 1);
insert into sys_user_role(user_id, role_id)
select u.id, r.id from sys_user u, sys_role r where u.username = 'admin' and r.code = 'ADMIN';
