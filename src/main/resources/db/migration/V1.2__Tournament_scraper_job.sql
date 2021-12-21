create table tournament_scraper_job
(
    id            int generated always as identity primary key,
    tournament_id int not null,
    unique (tournament_id)
);