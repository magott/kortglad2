create table referee_visit
(
    id          int generated always as identity primary key,
    referee_id  int not null references referee (fiks_id),
    timestamps  bigint[],
    total_visit int generated always as ( array_length(timestamps, 1) ) stored,
    unique (referee_id)
);