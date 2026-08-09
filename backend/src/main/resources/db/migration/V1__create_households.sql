create table households
(
    id         uuid        not null,
    name       varchar(200) not null,
    notes      varchar(2000),
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    constraint pk_households primary key (id)
);