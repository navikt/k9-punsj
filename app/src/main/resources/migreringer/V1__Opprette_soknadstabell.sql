create table if not exists soknader (
    id_soknad           serial      primary key,
    id_mappe            uuid        not null,
    norsk_ident         char (11)   not null,
    sist_endret         timestamp   not null,
    barn_norsk_ident    char (11),
    barn_fodselsdato    date,
    soknad              json
)