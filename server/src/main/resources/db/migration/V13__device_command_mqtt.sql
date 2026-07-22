create table device_commands (
    id uuid primary key default gen_random_uuid(),
    branch_id uuid not null,
    station_id uuid not null,
    device_id uuid not null,
    user_id uuid,
    play_session_id uuid,
    correlation_id uuid not null,
    command_type varchar(50) not null,
    command_value varchar(200) not null,
    unit varchar(30),
    status varchar(30) not null default 'PENDING',
    attempt_count integer not null default 0,
    max_attempts integer not null default 1,
    dangerous boolean not null default false,
    emergency boolean not null default false,
    mqtt_topic varchar(500) not null,
    sent_at timestamp with time zone,
    acknowledged_at timestamp with time zone,
    timed_out_at timestamp with time zone,
    result_message varchar(1000),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_device_commands_correlation_id unique (correlation_id),
    constraint fk_device_commands_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_device_commands_station foreign key (station_id) references stations (id) on delete restrict,
    constraint fk_device_commands_device foreign key (device_id) references iot_devices (id) on delete restrict,
    constraint fk_device_commands_user foreign key (user_id) references users (id) on delete set null,
    constraint fk_device_commands_session foreign key (play_session_id) references play_sessions (id) on delete set null,
    constraint ck_device_commands_attempts check (attempt_count >= 0 and max_attempts >= 1),
    constraint ck_device_commands_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table command_history (
    id uuid primary key default gen_random_uuid(),
    command_id uuid not null,
    actor_id uuid,
    from_status varchar(30),
    to_status varchar(30),
    action varchar(80) not null,
    note varchar(1000),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_command_history_command foreign key (command_id) references device_commands (id) on delete restrict,
    constraint fk_command_history_actor foreign key (actor_id) references users (id) on delete set null
);

create index idx_device_commands_station_status on device_commands (station_id, status, created_at desc);
create index idx_device_commands_device_status on device_commands (device_id, status, created_at desc);
create index idx_device_commands_sent_timeout on device_commands (status, sent_at);
create index idx_command_history_command on command_history (command_id, created_at);
