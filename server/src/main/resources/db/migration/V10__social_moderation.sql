create table user_blocks (
    id uuid primary key default gen_random_uuid(),
    blocker_id uuid not null,
    blocked_user_id uuid not null,
    reason varchar(500),
    blocked_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_user_block unique (blocker_id, blocked_user_id),
    constraint fk_user_blocks_blocker foreign key (blocker_id) references users (id) on delete restrict,
    constraint fk_user_blocks_blocked_user foreign key (blocked_user_id) references users (id) on delete restrict,
    constraint ck_user_blocks_not_self check (blocker_id <> blocked_user_id),
    constraint ck_user_blocks_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table user_reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id uuid not null,
    reported_user_id uuid not null,
    branch_id uuid,
    reason varchar(1000) not null,
    context varchar(1000),
    status varchar(30) not null default 'OPEN',
    moderation_action varchar(30),
    moderation_note varchar(1000),
    moderator_id uuid,
    reported_at timestamp with time zone,
    moderated_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_user_reports_reporter foreign key (reporter_id) references users (id) on delete restrict,
    constraint fk_user_reports_reported_user foreign key (reported_user_id) references users (id) on delete restrict,
    constraint fk_user_reports_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_user_reports_moderator foreign key (moderator_id) references users (id) on delete restrict,
    constraint ck_user_reports_not_self check (reporter_id <> reported_user_id),
    constraint ck_user_reports_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_user_blocks_blocker_id on user_blocks (blocker_id);
create index idx_user_blocks_blocked_user_id on user_blocks (blocked_user_id);
create index idx_user_reports_reporter_id on user_reports (reporter_id);
create index idx_user_reports_reported_user_id on user_reports (reported_user_id);
create index idx_user_reports_branch_status on user_reports (branch_id, status, reported_at desc);
