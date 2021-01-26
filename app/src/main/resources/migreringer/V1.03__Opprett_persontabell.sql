create table if not exists personer (
    person_id    uuid primary key,
    aktoer_ident char(20)  not null,
    person_ident char(11)  not null,
    sist_endret  timestamp not null
);

CREATE UNIQUE INDEX personer_uidx ON personer (aktoer_ident, person_ident)
