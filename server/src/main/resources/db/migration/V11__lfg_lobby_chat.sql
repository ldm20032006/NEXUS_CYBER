create table lfg_signals (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    play_session_id uuid,
    branch_id uuid,
    zone_id uuid,
    game_id uuid not null,
    rank_id uuid,
    role_id uuid,
    target_members integer not null,
    message varchar(1000),
    status varchar(30) not null default 'ACTIVE',
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_lfg_signals_user foreign key (user_id) references users (id) on delete restrict,
    constraint fk_lfg_signals_play_session foreign key (play_session_id) references play_sessions (id) on delete restrict,
    constraint fk_lfg_signals_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_lfg_signals_zone foreign key (zone_id) references zones (id) on delete set null,
    constraint fk_lfg_signals_game foreign key (game_id) references games (id) on delete restrict,
    constraint fk_lfg_signals_rank foreign key (rank_id) references game_ranks (id) on delete set null,
    constraint fk_lfg_signals_role foreign key (role_id) references game_roles (id) on delete set null,
    constraint ck_lfg_signals_target_members check (target_members >= 2),
    constraint ck_lfg_signals_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create unique index uk_lfg_signals_active_user
    on lfg_signals (user_id)
    where status = 'ACTIVE' and deleted = false;

create table lobbies (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null,
    leader_id uuid not null,
    game_id uuid not null,
    branch_id uuid not null,
    zone_id uuid,
    name varchar(150) not null,
    max_members integer not null,
    status varchar(30) not null default 'OPEN',
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_lobbies_creator foreign key (creator_id) references users (id) on delete restrict,
    constraint fk_lobbies_leader foreign key (leader_id) references users (id) on delete restrict,
    constraint fk_lobbies_game foreign key (game_id) references games (id) on delete restrict,
    constraint fk_lobbies_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_lobbies_zone foreign key (zone_id) references zones (id) on delete set null,
    constraint ck_lobbies_max_members check (max_members >= 2),
    constraint ck_lobbies_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table team_invitations (
    id uuid primary key default gen_random_uuid(),
    sender_id uuid not null,
    receiver_id uuid not null,
    lobby_id uuid,
    message varchar(1000),
    status varchar(30) not null default 'PENDING',
    expires_at timestamp with time zone not null,
    responded_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_team_invitations_sender foreign key (sender_id) references users (id) on delete restrict,
    constraint fk_team_invitations_receiver foreign key (receiver_id) references users (id) on delete restrict,
    constraint fk_team_invitations_lobby foreign key (lobby_id) references lobbies (id) on delete set null,
    constraint ck_team_invitations_not_self check (sender_id <> receiver_id),
    constraint ck_team_invitations_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create unique index uk_team_invitations_pending
    on team_invitations (sender_id, receiver_id)
    where status = 'PENDING' and deleted = false;

create table lobby_members (
    id uuid primary key default gen_random_uuid(),
    lobby_id uuid not null,
    user_id uuid not null,
    role varchar(30) not null default 'MEMBER',
    status varchar(30) not null default 'ACTIVE',
    joined_at timestamp with time zone not null,
    left_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_lobby_member unique (lobby_id, user_id),
    constraint fk_lobby_members_lobby foreign key (lobby_id) references lobbies (id) on delete restrict,
    constraint fk_lobby_members_user foreign key (user_id) references users (id) on delete restrict,
    constraint ck_lobby_members_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table lobby_messages (
    id uuid primary key default gen_random_uuid(),
    lobby_id uuid not null,
    sender_id uuid not null,
    message_type varchar(30) not null default 'TEXT',
    content varchar(2000) not null,
    sent_at timestamp with time zone not null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_lobby_messages_lobby foreign key (lobby_id) references lobbies (id) on delete restrict,
    constraint fk_lobby_messages_sender foreign key (sender_id) references users (id) on delete restrict,
    constraint ck_lobby_messages_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    type varchar(50) not null,
    channel varchar(30) not null default 'IN_APP',
    title varchar(200) not null,
    content varchar(2000) not null,
    read_at timestamp with time zone,
    entity_type varchar(50),
    entity_id varchar(100),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_notifications_user foreign key (user_id) references users (id) on delete restrict,
    constraint ck_notifications_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_lfg_signals_search on lfg_signals (branch_id, game_id, rank_id, role_id, zone_id, status, expires_at);
create index idx_team_invitations_receiver on team_invitations (receiver_id, status, created_at desc);
create index idx_team_invitations_sender on team_invitations (sender_id, status, created_at desc);
create index idx_lobbies_branch_status on lobbies (branch_id, status);
create index idx_lobby_members_lobby_status on lobby_members (lobby_id, status);
create index idx_lobby_messages_lobby_sent_at on lobby_messages (lobby_id, sent_at desc);
create index idx_notifications_user_read on notifications (user_id, read_at, created_at desc);
