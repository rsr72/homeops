create table vehicles
(
    id               uuid           not null,
    household_id     uuid           not null,
    make             varchar(200)   not null,
    model            varchar(200)   not null,
    year             integer        not null,
    vin              varchar(17),
    notes            varchar(2000),
    purchase_date    date,
    purchase_cost    numeric(12,2),
    current_mileage  integer,
    created_at       timestamp(6) with time zone not null,
    updated_at       timestamp(6) with time zone not null,
    constraint pk_vehicles primary key (id),
    constraint fk_vehicles_household foreign key (household_id) references households (id) on delete cascade
);

create index ix_vehicles_household_created_at on vehicles (household_id, created_at);