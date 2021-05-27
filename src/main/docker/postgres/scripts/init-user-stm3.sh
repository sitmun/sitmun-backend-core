#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER stm3 PASSWORD 'stm3';
    CREATE DATABASE stm3;
    GRANT ALL PRIVILEGES ON DATABASE stm3 TO stm3;
EOSQL
