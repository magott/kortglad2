create table referee
(
    fiks_id int primary key,
    name    text,
    name_tsv tsvector generated always as (to_tsvector('simple', name)) stored,
    last_sync timestamptz
);

create index on referee using gin(name_tsv);

create table referee_season
(
    id         int generated always as identity primary key,
    referee_id int not null references referee(fiks_id),
    year       int,
    matches    jsonb,
    unique (referee_id, year)
);