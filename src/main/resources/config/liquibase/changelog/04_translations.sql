-- Languages
INSERT INTO STM_LANGUAGE
VALUES (1, 'spa', 'Español');

INSERT INTO STM_SEQUENCE(SEQ_NAME, SEQ_COUNT)
VALUES ('LAN_ID', 1);

-- Translations
INSERT INTO STM_TRANSLATION
VALUES (1, 31, 'description', 1, 'Grupo de cartografía');
INSERT INTO STM_TRANSLATION
VALUES (2, 32, 'description', 1, 'Mapa de fondo');
INSERT INTO STM_TRANSLATION
VALUES (3, 33, 'description', 1, 'Mapa de situación');
INSERT INTO STM_TRANSLATION
VALUES (4, 34, 'description', 1, 'Informe');

INSERT INTO STM_SEQUENCE(SEQ_NAME, SEQ_COUNT)
VALUES ('TRA_ID', 4);