create table match_scraper_job
(
    id       int generated always as identity primary key,
    match_id int not null,
    unique (match_id)
);