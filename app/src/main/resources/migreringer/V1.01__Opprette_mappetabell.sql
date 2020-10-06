drop table soknader;
create table if not exists mappe
(
    id          uuid primary key,
    sist_endret timestamp not null,
    data        jsonb
);