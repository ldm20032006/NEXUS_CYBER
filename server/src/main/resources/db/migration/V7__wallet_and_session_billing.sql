create table wallets (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    balance numeric(19,2) not null default 0,
    currency varchar(10) not null default 'VND',
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_wallets_user_id unique (user_id),
    constraint fk_wallets_user foreign key (user_id) references users (id) on delete restrict,
    constraint ck_wallets_balance_non_negative check (balance >= 0),
    constraint ck_wallets_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table wallet_transactions (
    id uuid primary key default gen_random_uuid(),
    wallet_id uuid not null,
    user_id uuid not null,
    type varchar(30) not null,
    amount numeric(19,2) not null,
    currency varchar(10) not null default 'VND',
    balance_before numeric(19,2) not null,
    balance_after numeric(19,2) not null,
    reference_type varchar(50),
    reference_id varchar(100),
    original_transaction_id uuid,
    idempotency_key varchar(120),
    description varchar(1000),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_wallet_transactions_idempotency_key unique (idempotency_key),
    constraint fk_wallet_transactions_wallet foreign key (wallet_id) references wallets (id) on delete restrict,
    constraint fk_wallet_transactions_user foreign key (user_id) references users (id) on delete restrict,
    constraint fk_wallet_transactions_original foreign key (original_transaction_id) references wallet_transactions (id) on delete restrict,
    constraint ck_wallet_transactions_non_zero_amount check (amount <> 0),
    constraint ck_wallet_transactions_balance_after_non_negative check (balance_after >= 0),
    constraint ck_wallet_transactions_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table session_billing_policies (
    id uuid primary key default gen_random_uuid(),
    branch_id uuid not null,
    zone_id uuid,
    station_id uuid,
    hourly_rate numeric(19,2) not null,
    minimum_charge numeric(19,2) not null default 0,
    billing_increment_minutes integer not null default 1,
    active boolean not null default true,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_session_billing_policies_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_session_billing_policies_zone foreign key (zone_id) references zones (id) on delete restrict,
    constraint fk_session_billing_policies_station foreign key (station_id) references stations (id) on delete restrict,
    constraint ck_session_billing_policies_rate_non_negative check (hourly_rate >= 0 and minimum_charge >= 0),
    constraint ck_session_billing_policies_increment_positive check (billing_increment_minutes > 0),
    constraint ck_session_billing_policies_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create unique index uk_wallet_transactions_session_charge
    on wallet_transactions (reference_type, reference_id, type)
    where type = 'SESSION_CHARGE' and reference_type = 'PLAY_SESSION';

create index idx_wallet_transactions_wallet_id on wallet_transactions (wallet_id);
create index idx_wallet_transactions_user_id on wallet_transactions (user_id);
create index idx_wallet_transactions_type on wallet_transactions (type);
create index idx_wallet_transactions_reference on wallet_transactions (reference_type, reference_id);
create index idx_wallet_transactions_original_transaction_id on wallet_transactions (original_transaction_id);
create index idx_session_billing_policies_branch_id on session_billing_policies (branch_id);
create index idx_session_billing_policies_zone_id on session_billing_policies (zone_id);
create index idx_session_billing_policies_station_id on session_billing_policies (station_id);

create or replace function prevent_wallet_transaction_mutation()
returns trigger as $$
begin
    raise exception 'wallet_transactions is append-only';
end;
$$ language plpgsql;

create trigger trg_wallet_transactions_no_update
before update on wallet_transactions
for each row execute function prevent_wallet_transaction_mutation();

create trigger trg_wallet_transactions_no_delete
before delete on wallet_transactions
for each row execute function prevent_wallet_transaction_mutation();
