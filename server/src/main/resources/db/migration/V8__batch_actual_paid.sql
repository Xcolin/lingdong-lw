alter table payroll_batch add column actual_paid tinyint not null default 0;
alter table payroll_batch add column actual_paid_at datetime;
alter table payroll_batch add column actual_paid_by bigint;

create index idx_batch_actual_paid on payroll_batch (actual_paid, actual_paid_at);
