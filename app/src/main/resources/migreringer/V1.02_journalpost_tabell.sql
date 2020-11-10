create table if not exists journalpost (
   JOURNALPOSTID   VARCHAR(100) NOT NULL PRIMARY KEY,
   DATA jsonb        NOT NULL
)
