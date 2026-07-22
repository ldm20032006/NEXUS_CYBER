create table payment_transactions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    provider varchar(50) not null,
    provider_transaction_id varchar(120) not null,
    status varchar(30) not null default 'PENDING',
    amount numeric(19,2) not null,
    currency varchar(10) not null default 'VND',
    idempotency_key varchar(120),
    checkout_url varchar(500),
    requested_at timestamp with time zone not null,
    processed_at timestamp with time zone,
    failure_reason varchar(500),
    wallet_transaction_id uuid,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_payment_transactions_provider_tx unique (provider, provider_transaction_id),
    constraint uk_payment_transactions_idempotency_key unique (idempotency_key),
    constraint fk_payment_transactions_user foreign key (user_id) references users (id) on delete restrict,
    constraint fk_payment_transactions_wallet_transaction foreign key (wallet_transaction_id) references wallet_transactions (id) on delete restrict,
    constraint ck_payment_transactions_amount_positive check (amount > 0),
    constraint ck_payment_transactions_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_payment_transactions_user_id on payment_transactions (user_id);
create index idx_payment_transactions_status on payment_transactions (status);
create index idx_payment_transactions_requested_at on payment_transactions (requested_at desc);
