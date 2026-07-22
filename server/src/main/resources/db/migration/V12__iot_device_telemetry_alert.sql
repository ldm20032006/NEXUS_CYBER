create table iot_devices (
    id uuid primary key default gen_random_uuid(),
    branch_id uuid not null,
    station_id uuid,
    device_type varchar(50) not null,
    serial_number varchar(100) not null,
    name varchar(150),
    firmware_version varchar(100),
    capabilities varchar(2000),
    status varchar(30) not null default 'ACTIVE',
    last_heartbeat_at timestamp with time zone,
    missed_heartbeat_count integer not null default 0,
    mechanical_command_locked boolean not null default false,
    ip_address varchar(100),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_iot_device_serial unique (serial_number),
    constraint fk_iot_devices_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_iot_devices_station foreign key (station_id) references stations (id) on delete set null,
    constraint ck_iot_devices_missed_heartbeat check (missed_heartbeat_count >= 0),
    constraint ck_iot_devices_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table device_telemetries (
    id uuid primary key default gen_random_uuid(),
    device_id uuid not null,
    branch_id uuid not null,
    station_id uuid,
    received_at timestamp with time zone not null,
    online boolean,
    battery_level integer,
    signal_strength integer,
    error_code varchar(100),
    firmware_version varchar(100),
    metric_key varchar(100),
    metric_value varchar(200),
    payload_json text,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_device_telemetries_device foreign key (device_id) references iot_devices (id) on delete restrict,
    constraint fk_device_telemetries_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_device_telemetries_station foreign key (station_id) references stations (id) on delete set null,
    constraint ck_device_telemetries_battery check (battery_level is null or (battery_level >= 0 and battery_level <= 100)),
    constraint ck_device_telemetries_signal check (signal_strength is null or (signal_strength >= 0 and signal_strength <= 100)),
    constraint ck_device_telemetries_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table device_alerts (
    id uuid primary key default gen_random_uuid(),
    device_id uuid not null,
    branch_id uuid not null,
    station_id uuid,
    alert_code varchar(100) not null,
    title varchar(150) not null,
    description varchar(2000),
    severity varchar(30) not null default 'MEDIUM',
    status varchar(30) not null default 'OPEN',
    acknowledged_by uuid,
    assigned_staff_id uuid,
    acknowledged_at timestamp with time zone,
    resolved_by uuid,
    resolved_at timestamp with time zone,
    closed_at timestamp with time zone,
    critical_mechanical_lock boolean not null default false,
    note varchar(1000),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_device_alerts_device foreign key (device_id) references iot_devices (id) on delete restrict,
    constraint fk_device_alerts_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_device_alerts_station foreign key (station_id) references stations (id) on delete set null,
    constraint fk_device_alerts_ack_by foreign key (acknowledged_by) references users (id) on delete set null,
    constraint fk_device_alerts_assigned_staff foreign key (assigned_staff_id) references users (id) on delete set null,
    constraint fk_device_alerts_resolved_by foreign key (resolved_by) references users (id) on delete set null,
    constraint ck_device_alerts_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table alert_history (
    id uuid primary key default gen_random_uuid(),
    alert_id uuid not null,
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
    constraint fk_alert_history_alert foreign key (alert_id) references device_alerts (id) on delete restrict,
    constraint fk_alert_history_actor foreign key (actor_id) references users (id) on delete set null
);

create unique index uk_device_alert_open
    on device_alerts (device_id, alert_code)
    where status in ('OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'REOPENED') and deleted = false;

create index idx_iot_devices_branch_status on iot_devices (branch_id, status);
create index idx_iot_devices_station on iot_devices (station_id);
create index idx_device_telemetries_device_time on device_telemetries (device_id, received_at desc);
create index idx_device_telemetries_branch_time on device_telemetries (branch_id, received_at desc);
create index idx_device_alerts_branch_status_severity on device_alerts (branch_id, status, severity, created_at desc);
create index idx_device_alerts_station_status on device_alerts (station_id, status);
create index idx_alert_history_alert on alert_history (alert_id, created_at);
