DROP USER c##stm3 CASCADE;
CREATE USER c##stm3
  IDENTIFIED BY smt3
  DEFAULT TABLESPACE users
  QUOTA 20M on users;
GRANT connect TO c##stm3;
GRANT create session TO c##stm3;
GRANT create table TO c##stm3;
GRANT create view TO c##stm3;
GRANT create any trigger TO c##stm3;
GRANT create any procedure TO c##stm3;
GRANT create sequence TO c##stm3;
GRANT create synonym TO c##stm3;
GRANT SELECT ON SYS.DBA_RECYCLEBIN TO C##STM3;
