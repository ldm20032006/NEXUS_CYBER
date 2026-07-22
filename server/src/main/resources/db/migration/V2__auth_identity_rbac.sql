create table branches (
    id uuid primary key default gen_random_uuid(),
    code varchar(50) not null,
    name varchar(150) not null,
    address varchar(500),
    timezone varchar(100),
    status varchar(30) not null default 'ACTIVE',
    payment_enabled boolean not null default true,
    operating_start_time time,
    operating_end_time time,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_branches_code unique (code),
    constraint ck_branches_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table permissions (
    id uuid primary key default gen_random_uuid(),
    code varchar(100) not null,
    name varchar(150) not null,
    description varchar(500),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_permissions_code unique (code),
    constraint ck_permissions_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table roles (
    id uuid primary key default gen_random_uuid(),
    code varchar(50) not null,
    name varchar(150) not null,
    description varchar(500),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_roles_code unique (code),
    constraint ck_roles_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table role_permissions (
    role_id uuid not null,
    permission_id uuid not null,
    primary key (role_id, permission_id),
    constraint fk_role_permissions_role foreign key (role_id) references roles (id) on delete cascade,
    constraint fk_role_permissions_permission foreign key (permission_id) references permissions (id) on delete restrict
);

create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(150) not null,
    phone varchar(20),
    password_hash varchar(255) not null,
    full_name varchar(120) not null,
    display_name varchar(120),
    avatar_url varchar(500),
    status varchar(30) not null default 'PENDING',
    locked_at timestamp with time zone,
    lock_reason varchar(500),
    activated_at timestamp with time zone,
    last_login_at timestamp with time zone,
    branch_id uuid,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_users_email unique (email),
    constraint uk_users_phone unique (phone),
    constraint fk_users_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint ck_users_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table user_roles (
    user_id uuid not null,
    role_id uuid not null,
    primary key (user_id, role_id),
    constraint fk_user_roles_user foreign key (user_id) references users (id) on delete cascade,
    constraint fk_user_roles_role foreign key (role_id) references roles (id) on delete restrict
);

create table refresh_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    token_hash varchar(512) not null,
    family_id uuid not null,
    issued_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    reuse_detected_at timestamp with time zone,
    replaced_by_token_hash varchar(255),
    revoke_reason varchar(255),
    ip_address varchar(100),
    user_agent varchar(255),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_refresh_tokens_token_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id) references users (id) on delete cascade,
    constraint ck_refresh_tokens_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table password_reset_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    token_hash varchar(512) not null,
    issued_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_password_reset_tokens_token_hash unique (token_hash),
    constraint fk_password_reset_tokens_user foreign key (user_id) references users (id) on delete cascade,
    constraint ck_password_reset_tokens_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_users_branch_id on users (branch_id);
create index idx_users_status on users (status);
create index idx_refresh_tokens_user_id on refresh_tokens (user_id);
create index idx_refresh_tokens_family_id on refresh_tokens (family_id);
create index idx_refresh_tokens_expires_at on refresh_tokens (expires_at);
create index idx_password_reset_tokens_user_id on password_reset_tokens (user_id);
create index idx_password_reset_tokens_expires_at on password_reset_tokens (expires_at);

insert into permissions (code, name, description) values
    ('AUTH_SELF', 'Current user access', 'Read and manage own authentication state'),
    ('USER_ADMIN', 'User administration', 'Create, lock, activate and list users'),
    ('BRANCH_SCOPE', 'Branch scoped access', 'Access records within assigned branch')
on conflict (code) do nothing;

insert into roles (code, name, description) values
    ('GAMER', 'Gamer', 'Customer player account'),
    ('STAFF_FNB', 'F&B Staff', 'Food and beverage staff'),
    ('STAFF_TECHNICAL', 'Technical Staff', 'Technical support staff'),
    ('BRANCH_ADMIN', 'Branch Admin', 'Branch administrator'),
    ('SUPER_ADMIN', 'Super Admin', 'System-wide administrator'),
    ('STATION_CLIENT', 'Station Client', 'Station client terminal')
on conflict (code) do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code = 'AUTH_SELF'
where r.code in ('GAMER', 'STAFF_FNB', 'STAFF_TECHNICAL', 'BRANCH_ADMIN', 'SUPER_ADMIN', 'STATION_CLIENT')
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in ('USER_ADMIN', 'BRANCH_SCOPE')
where r.code in ('BRANCH_ADMIN', 'SUPER_ADMIN')
on conflict do nothing;
