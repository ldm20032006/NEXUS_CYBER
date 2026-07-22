alter table notifications add column branch_id varchar(100);
alter table notifications add column metadata_json varchar(1000);
alter table notifications add column hidden_at timestamp with time zone;

create table push_subscriptions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    endpoint_hash varchar(128) not null,
    endpoint varchar(2000) not null,
    p256dh varchar(500) not null,
    auth varchar(500) not null,
    user_agent varchar(100),
    last_used_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_push_subscription_endpoint_hash unique (endpoint_hash),
    constraint fk_push_subscriptions_user foreign key (user_id) references users (id) on delete restrict,
    constraint ck_push_subscriptions_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table notification_deliveries (
    id uuid primary key default gen_random_uuid(),
    notification_id uuid not null,
    push_subscription_id uuid,
    channel varchar(30) not null,
    status varchar(30) not null default 'PENDING',
    attempt_count integer not null default 0,
    max_attempts integer not null default 3,
    last_attempt_at timestamp with time zone,
    next_attempt_at timestamp with time zone,
    failure_reason varchar(1000),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_notification_deliveries_notification foreign key (notification_id) references notifications (id) on delete restrict,
    constraint fk_notification_deliveries_push_subscription foreign key (push_subscription_id) references push_subscriptions (id) on delete set null,
    constraint ck_notification_deliveries_attempts check (attempt_count >= 0 and max_attempts >= 1),
    constraint ck_notification_deliveries_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_notifications_user_hidden on notifications (user_id, hidden_at, created_at desc);
create index idx_push_subscriptions_user on push_subscriptions (user_id, deleted);
create index idx_notification_deliveries_notification on notification_deliveries (notification_id, created_at);
create index idx_notification_deliveries_retry on notification_deliveries (status, next_attempt_at, attempt_count);
