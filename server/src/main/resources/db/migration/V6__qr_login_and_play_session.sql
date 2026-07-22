create table qr_login_sessions (
    id uuid primary key default gen_random_uuid(),
    station_id uuid not null,
    nonce varchar(100) not null,
    qr_payload varchar(500) not null,
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    status varchar(30) not null default 'PENDING',
    idempotency_key varchar(100),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_qr_nonce unique (nonce),
    constraint uk_qr_login_sessions_idempotency_key unique (idempotency_key),
    constraint fk_qr_login_sessions_station foreign key (station_id) references stations (id) on delete restrict,
    constraint ck_qr_login_sessions_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table play_sessions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    station_id uuid not null,
    qr_login_session_id uuid,
    status varchar(30) not null default 'PENDING',
    started_at timestamp with time zone not null,
    ended_at timestamp with time zone,
    duration_minutes integer,
    estimated_cost numeric(19,2) default 0,
    actual_cost numeric(19,2) default 0,
    start_balance numeric(19,2) default 0,
    end_balance numeric(19,2) default 0,
    ended_reason varchar(500),
    idempotency_key varchar(100),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_play_sessions_idempotency_key unique (idempotency_key),
    constraint fk_play_sessions_user foreign key (user_id) references users (id) on delete restrict,
    constraint fk_play_sessions_station foreign key (station_id) references stations (id) on delete restrict,
    constraint fk_play_sessions_qr_login_session foreign key (qr_login_session_id) references qr_login_sessions (id) on delete restrict,
    constraint ck_play_sessions_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create unique index uk_play_sessions_active_user
    on play_sessions (user_id)
    where status = 'ACTIVE' and deleted = false;

create unique index uk_play_sessions_active_station
    on play_sessions (station_id)
    where status = 'ACTIVE' and deleted = false;

create index idx_qr_login_sessions_station_id on qr_login_sessions (station_id);
create index idx_qr_login_sessions_status on qr_login_sessions (status);
create index idx_qr_login_sessions_expires_at on qr_login_sessions (expires_at);
create index idx_play_sessions_user_id on play_sessions (user_id);
create index idx_play_sessions_station_id on play_sessions (station_id);
create index idx_play_sessions_status on play_sessions (status);
create index idx_play_sessions_started_at on play_sessions (started_at desc);
