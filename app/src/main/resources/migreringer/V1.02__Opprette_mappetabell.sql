CREATE TABLE IF NOT EXISTS MAPPE
(
    ID            UUID PRIMARY KEY,
    ID_PERSON     UUID                                   NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'PUNSJ'           NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    CONSTRAINT FK_MAPPE_1
        FOREIGN KEY (ID_PERSON)
            REFERENCES PERSON (PERSON_ID)
);
