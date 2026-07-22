create table gamer_profiles (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    nickname varchar(120),
    avatar_url varchar(500),
    date_of_birth date,
    height_cm integer,
    weight_kg integer,
    night_mode boolean default false,
    bio varchar(1000),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_gamer_profiles_user unique (user_id),
    constraint fk_gamer_profiles_user foreign key (user_id) references users (id) on delete cascade,
    constraint ck_gamer_profiles_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table station_preferences (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    desk_height_cm integer,
    chair_angle_degree integer,
    rgb_color varchar(20),
    brightness integer,
    mouse_dpi integer,
    night_mode boolean default false,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_station_preferences_user unique (user_id),
    constraint fk_station_preferences_user foreign key (user_id) references users (id) on delete cascade,
    constraint ck_station_preferences_desk check (desk_height_cm is null or desk_height_cm between 60 and 120),
    constraint ck_station_preferences_chair check (chair_angle_degree is null or chair_angle_degree between 90 and 145),
    constraint ck_station_preferences_rgb check (rgb_color is null or rgb_color ~ '^#[0-9A-Fa-f]{6}$'),
    constraint ck_station_preferences_brightness check (brightness is null or brightness between 0 and 100),
    constraint ck_station_preferences_dpi check (mouse_dpi is null or mouse_dpi between 200 and 32000),
    constraint ck_station_preferences_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table games (
    id uuid primary key default gen_random_uuid(),
    slug varchar(100) not null,
    name varchar(150) not null,
    description varchar(500),
    max_lobby_size integer not null default 5,
    status varchar(30) not null default 'ACTIVE',
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_games_slug unique (slug),
    constraint ck_games_max_lobby_size check (max_lobby_size between 1 and 20),
    constraint ck_games_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table game_ranks (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null,
    code varchar(50) not null,
    name varchar(100) not null,
    sort_order integer,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_game_rank_code unique (game_id, code),
    constraint fk_game_ranks_game foreign key (game_id) references games (id) on delete cascade,
    constraint ck_game_ranks_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table game_roles (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null,
    code varchar(50) not null,
    name varchar(100) not null,
    sort_order integer,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_game_role_code unique (game_id, code),
    constraint fk_game_roles_game foreign key (game_id) references games (id) on delete cascade,
    constraint ck_game_roles_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table gamer_game_profiles (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    game_id uuid not null,
    in_game_name varchar(120),
    rank_id uuid,
    preferred_role_id uuid,
    secondary_role_id uuid,
    play_style varchar(1000),
    short_description varchar(1000),
    visible_on_radar boolean default true,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_gamer_game_profile unique (user_id, game_id),
    constraint fk_gamer_game_profiles_user foreign key (user_id) references users (id) on delete cascade,
    constraint fk_gamer_game_profiles_game foreign key (game_id) references games (id) on delete restrict,
    constraint fk_gamer_game_profiles_rank foreign key (rank_id) references game_ranks (id) on delete restrict,
    constraint fk_gamer_game_profiles_preferred_role foreign key (preferred_role_id) references game_roles (id) on delete restrict,
    constraint fk_gamer_game_profiles_secondary_role foreign key (secondary_role_id) references game_roles (id) on delete restrict,
    constraint ck_gamer_game_profiles_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_gamer_profiles_user_id on gamer_profiles (user_id);
create index idx_station_preferences_user_id on station_preferences (user_id);
create index idx_games_status on games (status);
create index idx_game_ranks_game_id on game_ranks (game_id);
create index idx_game_roles_game_id on game_roles (game_id);
create index idx_gamer_game_profiles_user_id on gamer_game_profiles (user_id);
create index idx_gamer_game_profiles_game_id on gamer_game_profiles (game_id);
