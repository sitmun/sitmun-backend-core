--liquibase formatted sql
--changeset sitmun:3 context:dev

INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (50, 'Name of Connection 50', 'driver.jdbc.num50', 'User50', null, 'jdbc:database:@host50');
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (28, 'Name of Connection 28', 'driver.jdbc.num28', null, null, 'jdbc:database:@host28');
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (29, 'Name of Connection 29', 'driver.jdbc.num29', 'User29', 'Password29', null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (30, 'Name of Connection 30', 'driver.jdbc.num30', null, 'Password30', null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (31, 'Name of Connection 31', 'driver.jdbc.num31', 'User31', null, null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (3, 'Name of Connection 3', 'driver.jdbc.num3', null, null, null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (2, 'Name of Connection 2', 'driver.jdbc.num2', 'User2', 'Password2', 'jdbc:database:@host2');
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (4, 'Name of Connection 4', 'driver.jdbc.num4', null, 'Password4', 'jdbc:database:@host4');
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (5, 'Name of Connection 5', 'driver.jdbc.num5', 'User5', null, 'jdbc:database:@host5');
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (6, 'Name of Connection 6', 'driver.jdbc.num6', null, null, 'jdbc:database:@host6');
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (20, 'Name of Connection 20', 'driver.jdbc.num20', 'User20', 'Password20', null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (26, 'Name of Connection 26', 'driver.jdbc.num26', null, 'Password26', null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (21, 'Name of Connection 21', 'driver.jdbc.num21', 'User21', null, null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (27, 'Name of Connection 27', 'driver.jdbc.num27', null, null, null);
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (51, 'Name of Connection 51', 'driver.jdbc.num51', 'User51', 'Password51', 'jdbc:database:@host51');
INSERT INTO STM_CONNECT(CON_ID, CON_NAME, CON_DRIVER, CON_USER, CON_PWD, CON_CONNECTION)
VALUES (52, 'Name of Connection 52', 'driver.jdbc.num52', null, 'Password52', 'jdbc:database:@host52');

INSERT INTO STM_SEQUENCE(SEQ_NAME, SEQ_COUNT)
VALUES ('CON_ID', 52);
