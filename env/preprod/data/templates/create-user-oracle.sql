-- Create schema ORACLE
CREATE USER ${user} IDENTIFIED BY ${password} DEFAULT TABLESPACE ${tablespace} QUOTA 20M ON ${tablespace};
GRANT connect TO ${user};
GRANT create session TO ${user};
GRANT create table TO ${user};
GRANT create view TO ${user};
GRANT create any trigger TO ${user};
GRANT create any procedure TO ${user};
GRANT create sequence TO ${user};
GRANT create synonym TO ${user};
GRANT SELECT ON SYS.DBA_RECYCLEBIN TO ${user};
