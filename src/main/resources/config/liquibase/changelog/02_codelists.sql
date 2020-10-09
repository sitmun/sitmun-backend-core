--liquibase formatted sql

--changeset sitmun:2

INSERT INTO STM_CODELIST VALUES  (1, 'legendType', 'LINK', '');
INSERT INTO STM_CODELIST VALUES  (2, 'legendType', 'LEGENDGRAPHIC', '');
INSERT INTO STM_CODELIST VALUES  (3, 'legendType', 'CAPABILITIES', '');
