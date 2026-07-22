create table audit_logs (
    id uuid primary key default gen_random_uuid(),
    actor_id uuid,
    actor_role varchar(50),
    branch_id uuid,
    action varchar(50) not null,
    resource_type varchar(100) not null,
    resource_id varchar(100),
    before_data text,
    after_data text,
    ip_address varchar(100),
    user_agent varchar(255),
    correlation_id varchar(100),
    event_timestamp timestamp with time zone not null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_audit_logs_actor foreign key (actor_id) references users (id) on delete set null,
    constraint fk_audit_logs_branch foreign key (branch_id) references branches (id) on delete set null,
    constraint ck_audit_logs_not_soft_deleted check (deleted = false and deleted_at is null)
);

create index idx_audit_logs_actor_id on audit_logs (actor_id);
create index idx_audit_logs_branch_id on audit_logs (branch_id);
create index idx_audit_logs_action on audit_logs (action);
create index idx_audit_logs_resource on audit_logs (resource_type, resource_id);
create index idx_audit_logs_event_timestamp on audit_logs (event_timestamp desc);
create index idx_audit_logs_correlation_id on audit_logs (correlation_id);

create or replace function prevent_audit_logs_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'audit_logs is append-only';
end;
$$;

create trigger trg_audit_logs_prevent_update
before update on audit_logs
for each row execute function prevent_audit_logs_mutation();

create trigger trg_audit_logs_prevent_delete
before delete on audit_logs
for each row execute function prevent_audit_logs_mutation();
