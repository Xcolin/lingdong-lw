alter table payroll_batch_item add column target_type varchar(16) not null default 'PERSON';
alter table payroll_batch_item add column unit_id bigint null;
alter table payroll_batch_item modify column id_card_no varchar(32) null;

create index idx_batch_item_unit on payroll_batch_item (unit_id);
