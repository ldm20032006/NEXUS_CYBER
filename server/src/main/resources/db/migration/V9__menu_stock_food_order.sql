create table menu_categories (
    id uuid primary key default gen_random_uuid(),
    branch_id uuid not null,
    code varchar(50) not null,
    name varchar(150) not null,
    description varchar(500),
    sort_order integer,
    active boolean default true,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_menu_category_branch_code unique (branch_id, code),
    constraint fk_menu_categories_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint ck_menu_categories_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table menu_items (
    id uuid primary key default gen_random_uuid(),
    branch_id uuid not null,
    category_id uuid not null,
    code varchar(50) not null,
    name varchar(150) not null,
    description varchar(1000),
    image_url varchar(500),
    price numeric(19,2) not null,
    stock_quantity integer not null default 0,
    estimated_prep_minutes integer,
    status varchar(30) not null default 'ACTIVE',
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_menu_item_code unique (branch_id, code),
    constraint fk_menu_items_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_menu_items_category foreign key (category_id) references menu_categories (id) on delete restrict,
    constraint ck_menu_items_price_non_negative check (price >= 0),
    constraint ck_menu_items_stock_non_negative check (stock_quantity >= 0),
    constraint ck_menu_items_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table food_orders (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    branch_id uuid not null,
    station_id uuid,
    play_session_id uuid not null,
    status varchar(30) not null default 'NEW',
    payment_method varchar(30) not null default 'WALLET',
    total_amount numeric(19,2) not null default 0,
    note varchar(1000),
    cancel_reason varchar(500),
    idempotency_key varchar(100),
    payment_wallet_transaction_id uuid,
    refund_wallet_transaction_id uuid,
    accepted_at timestamp with time zone,
    preparing_at timestamp with time zone,
    ready_at timestamp with time zone,
    delivered_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint uk_food_orders_idempotency_key unique (idempotency_key),
    constraint fk_food_orders_user foreign key (user_id) references users (id) on delete restrict,
    constraint fk_food_orders_branch foreign key (branch_id) references branches (id) on delete restrict,
    constraint fk_food_orders_station foreign key (station_id) references stations (id) on delete restrict,
    constraint fk_food_orders_play_session foreign key (play_session_id) references play_sessions (id) on delete restrict,
    constraint fk_food_orders_payment_wallet_transaction foreign key (payment_wallet_transaction_id) references wallet_transactions (id) on delete restrict,
    constraint fk_food_orders_refund_wallet_transaction foreign key (refund_wallet_transaction_id) references wallet_transactions (id) on delete restrict,
    constraint ck_food_orders_total_non_negative check (total_amount >= 0),
    constraint ck_food_orders_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create table order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null,
    menu_item_id uuid not null,
    item_name_snapshot varchar(150) not null,
    unit_price numeric(19,2) not null,
    quantity integer not null,
    note varchar(1000),
    line_total numeric(19,2) not null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    created_by uuid,
    updated_by uuid,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    version bigint not null default 0,
    constraint fk_order_items_order foreign key (order_id) references food_orders (id) on delete restrict,
    constraint fk_order_items_menu_item foreign key (menu_item_id) references menu_items (id) on delete restrict,
    constraint ck_order_items_quantity_positive check (quantity > 0),
    constraint ck_order_items_money_non_negative check (unit_price >= 0 and line_total >= 0),
    constraint ck_order_items_soft_delete check ((deleted = false and deleted_at is null) or (deleted = true and deleted_at is not null))
);

create index idx_menu_categories_branch_id on menu_categories (branch_id);
create index idx_menu_items_branch_id on menu_items (branch_id);
create index idx_menu_items_category_id on menu_items (category_id);
create index idx_menu_items_status on menu_items (status);
create index idx_food_orders_user_id on food_orders (user_id);
create index idx_food_orders_branch_status on food_orders (branch_id, status, created_at);
create index idx_food_orders_play_session_id on food_orders (play_session_id);
create index idx_order_items_order_id on order_items (order_id);
create index idx_order_items_menu_item_id on order_items (menu_item_id);
