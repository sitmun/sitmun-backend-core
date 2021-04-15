--liquibase formatted sql
--changeset sitmun:6

INSERT INTO STM_CONF
VALUES (1, 'language.default', 'en');


INSERT INTO STM_SEQUENCE(SEQ_NAME, SEQ_COUNT)
VALUES ('CNF_ID', 1);