alter table payee_person add column enabled tinyint not null default 1;
alter table paying_unit add column enabled tinyint not null default 1;

create index idx_payee_enabled on payee_person (enabled);
create index idx_unit_enabled on paying_unit (enabled);
