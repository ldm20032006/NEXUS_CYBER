alter table branches
    add column payment_policy varchar(50) not null default 'PREPAID_OR_WALLET';

create table zones (
    id uuid primary key default gen_random_uuid(),
    branch_id uuid not null,
    code varchar(50) not null,
    name varchar(150) not null,
    zone_type varchar(100),
    status varchar(30) not null default 'ACTIVE',
    sort_order integer,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_zone_branch_code unique (branch_id, code),
    constraint fk_zones_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint ck_zones_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table stations (
    id uuid primary key default gen_random_uuid(),
    branch_id uuid not null,
    zone_id uuid,
    station_number integer not null,
    code varchar(50) not null,
    name varchar(150) not null,
    status varchar(30) not null default 'AVAILABLE',
    ip_address varchar(100),
    mac_address varchar(100),
    last_seen_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_station_branch_code unique (branch_id, code),
    constraint uk_station_branch_number unique (branch_id, station_number),
    constraint fk_stations_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_stations_zone foreign key (zone_id) references zones (id) on delete set null,
    constraint ck_stations_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table station_credentials (
    id uuid primary key default gen_random_uuid(),
    station_id uuid not null,
    secret_hash varchar(512) not null,
    issued_at timestamp with time zone not null,
    expires_at timestamp with time zone,
    revoked_at timestamp with time zone,
    last_used_at timestamp with time zone,
    revoke_reason varchar(255),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_station_credentials_secret_hash unique (secret_hash),
    constraint fk_station_credentials_station foreign key (station_id) references stations (id) on delete cascade,
    constraint ck_station_credentials_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_zones_branch_id on zones (branch_id);
create index idx_zones_status on zones (status);
create index idx_stations_branch_id on stations (branch_id);
create index idx_stations_zone_id on stations (zone_id);
create index idx_stations_status on stations (status);
create index idx_stations_last_seen_at on stations (last_seen_at);
create index idx_station_credentials_station_id on station_credentials (station_id);
create index idx_station_credentials_revoked_at on station_credentials (revoked_at);
